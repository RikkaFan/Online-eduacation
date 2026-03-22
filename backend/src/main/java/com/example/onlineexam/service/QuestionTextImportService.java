package com.example.onlineexam.service;

import com.example.onlineexam.model.Question;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionTextImportService {

    private static final Pattern QUESTION_BLOCK_PATTERN = Pattern.compile("(?s)(?:^|\\n)\\s*(\\d+)[\\.、]\\s*(.*?)(?=(?:\\n\\s*\\d+[\\.、])|$)");
    private static final Pattern ANSWER_PATTERN = Pattern.compile("(?:【?答案】?|答案)\\s*[:：]?\\s*([A-DTF](?:\\s*[,，]\\s*[A-D])*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANALYSIS_PATTERN = Pattern.compile("(?:【?解析】?|解析)\\s*[:：]\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OPTION_PATTERN = Pattern.compile("([A-D])[\\.、:：]\\s*(.*?)(?=(?:\\s+[A-D][\\.、:：])|(?:【?答案】?)|(?:\\b答案\\b)|(?:【?解析】?)|(?:\\b解析\\b)|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public ParseResult parse(String rawText, Long courseId) {
        ParseResult result = new ParseResult();
        if (rawText == null || rawText.trim().isEmpty()) {
            result.errors.add("输入内容为空");
            return result;
        }
        String normalized = normalize(rawText);
        Matcher blockMatcher = QUESTION_BLOCK_PATTERN.matcher(normalized);
        while (blockMatcher.find()) {
            String qNo = blockMatcher.group(1);
            String block = blockMatcher.group(2);
            try {
                Question question = parseQuestionBlock(block, courseId);
                if (question == null) {
                    result.errors.add("第" + qNo + "题解析失败：题干或答案缺失");
                    continue;
                }
                result.questions.add(question);
            } catch (Exception e) {
                result.errors.add("第" + qNo + "题解析失败：" + e.getMessage());
            }
        }
        if (result.questions.isEmpty() && result.errors.isEmpty()) {
            result.errors.add("未识别到题号格式，请检查是否为“1. ...”或“1、...”");
        }
        return result;
    }

    private Question parseQuestionBlock(String block, Long courseId) {
        String work = block == null ? "" : block.trim();
        if (work.isEmpty()) return null;

        Matcher analysisMatcher = ANALYSIS_PATTERN.matcher(work);
        if (analysisMatcher.find()) {
            work = work.substring(0, analysisMatcher.start()).trim();
        }

        String answer = "";
        Matcher answerMatcher = ANSWER_PATTERN.matcher(work);
        if (answerMatcher.find()) {
            answer = normalizeAnswer(answerMatcher.group(1));
            work = (work.substring(0, answerMatcher.start()) + " " + work.substring(answerMatcher.end())).trim();
        }

        Map<String, String> options = new LinkedHashMap<>();
        Matcher optionMatcher = OPTION_PATTERN.matcher(work);
        int firstOptionStart = -1;
        while (optionMatcher.find()) {
            if (firstOptionStart < 0) {
                firstOptionStart = optionMatcher.start();
            }
            String key = optionMatcher.group(1).toUpperCase(Locale.ROOT);
            String value = clean(optionMatcher.group(2));
            options.put(key, value);
        }

        String stem = firstOptionStart >= 0 ? work.substring(0, firstOptionStart) : work;
        stem = clean(stem);
        if (stem.isEmpty() || answer.isEmpty()) {
            return null;
        }

        String type = detectType(answer, options);
        Question question = new Question();
        question.setCourseId(courseId);
        question.setCategoryId(courseId);
        question.setContent(stem);
        question.setType(type);
        question.setAnswer(answer);
        if (!options.isEmpty()) {
            question.setOptions(String.format(
                    "A:%s, B:%s, C:%s, D:%s",
                    options.getOrDefault("A", ""),
                    options.getOrDefault("B", ""),
                    options.getOrDefault("C", ""),
                    options.getOrDefault("D", "")
            ));
        } else {
            question.setOptions("");
        }
        return question;
    }

    private String detectType(String answer, Map<String, String> options) {
        if ("T".equals(answer) || "F".equals(answer)) {
            return "JUDGE";
        }
        if (answer.contains(",")) {
            return "MULTIPLE";
        }
        if (options.isEmpty()) {
            return "SUBJECTIVE";
        }
        return "SINGLE";
    }

    private String normalizeAnswer(String answerRaw) {
        String raw = clean(answerRaw).toUpperCase(Locale.ROOT).replace('，', ',');
        if ("TRUE".equals(raw)) return "T";
        if ("FALSE".equals(raw)) return "F";
        if (raw.startsWith("对")) return "T";
        if (raw.startsWith("错")) return "F";
        String[] parts = raw.split(",");
        List<String> labels = new ArrayList<>();
        for (String p : parts) {
            String t = clean(p).replaceAll("[^A-Z]", "");
            if (!t.isEmpty()) {
                labels.add(t.substring(0, 1));
            }
        }
        if (!labels.isEmpty()) {
            return String.join(",", labels);
        }
        return raw;
    }

    private String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ')
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("[\\t\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    public static class ParseResult {
        private final List<Question> questions = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public List<Question> getQuestions() {
            return questions;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
