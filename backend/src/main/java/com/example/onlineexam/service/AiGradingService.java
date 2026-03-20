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

    public String explainWrongAnswer(String question, String standardAnswer, String studentAnswer) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "AI 私教暂时无法连接（未配置服务密钥）。先给你一个思路：先对照标准答案拆出关键得分点，再定位你答案中遗漏或混淆的部分，最后按“结论→依据→步骤”三段式重写一遍，正确率会明显提升。";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "你是一位极其幽默、循循善诱的资深金牌讲师。你的任务是给做错题的学生讲题。";
        String userPrompt = "题目是：【" + safe(question) + "】。标准答案是：【" + safe(standardAnswer)
                + "】。我选了（或填了）：【" + safe(studentAnswer)
                + "】。请告诉我为什么我错了，正确答案的解题思路是什么？语言尽量通俗易懂，像聊天一样，控制在 300 字以内。";

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            String content = extractContent(response.getBody());
            if (content == null || content.trim().isEmpty()) {
                return "AI 私教刚刚走神了，没返回有效讲解。建议你先看标准答案的关键词，再把自己的作答和关键词逐条对齐，补齐缺失点后再试一次。";
            }
            return content;
        } catch (Exception e) {
            return "AI 私教暂时繁忙，先给你一个速解法：先判断题目考点，再把标准答案拆成2-3个关键步骤，对照你的答案找出偏差点，最后按步骤重写。等会儿再点一次，我继续陪你练。";
        }
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
