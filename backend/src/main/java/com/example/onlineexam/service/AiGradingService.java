package com.example.onlineexam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiGradingService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api.key:}")
    private String apiKey;

    public AiGradingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Integer gradeSubjectiveQuestion(String questionContent, String standardAnswer, String studentAnswer, Integer maxScore) {
        int max = maxScore == null || maxScore < 0 ? 0 : maxScore;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("DeepSeek API Key 未配置");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "你是一个严格的阅卷老师。请根据标准答案对学生的回答进行打分。要求：只返回一个 0 到 "
                + max + " 之间的纯数字，不要包含任何其他文字或标点。";
        String userPrompt = "题目：" + safe(questionContent)
                + "\n标准答案：" + safe(standardAnswer)
                + "\n学生回答：" + safe(studentAnswer)
                + "\n满分：" + max;

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "temperature", 0.1,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
        String content = extractContent(response.getBody());
        int parsed = extractScore(content);
        if (parsed < 0) return 0;
        return Math.min(parsed, max);
    }

    private String extractContent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            return contentNode.isMissingNode() ? "" : contentNode.asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private int extractScore(String text) {
        if (text == null) return -1;
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (!matcher.find()) return -1;
        return Integer.parseInt(matcher.group(1));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
