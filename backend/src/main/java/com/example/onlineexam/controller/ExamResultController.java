package com.example.onlineexam.controller;

import com.example.onlineexam.model.ExamResult;
import com.example.onlineexam.model.Question;
import com.example.onlineexam.model.StudentAnswer;
import com.example.onlineexam.payload.response.ScoreExcelDTO;
import com.example.onlineexam.repository.ExamResultRepository;
import com.example.onlineexam.repository.StudentAnswerRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import com.example.onlineexam.service.AiGradingService;
import com.example.onlineexam.service.ExamResultService;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ExamResultController {

    @Autowired
    private ExamResultService examResultService;

    @Autowired
    private AiGradingService aiGradingService;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @PostMapping("/exams/{examId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitAnswers(@PathVariable Long examId, @RequestBody List<StudentAnswer> answers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            return ResponseEntity.status(401).body(Map.of("message", "登录状态失效，请重新登录后交卷"));
        }
        Long studentId = userDetails.getId();
        try {
            ExamResult result = examResultService.submitAnswers(examId, studentId, answers);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "提交失败：作答内容过长或数据不符合存储要求"));
        }
    }

    @GetMapping("/exams/{examId}/results")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<ExamResult> getResultsByExam(@PathVariable Long examId) {
        return examResultService.getResultsByExam(examId);
    }

    @GetMapping("/exams/{examId}/export")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public void exportResultsByExam(@PathVariable Long examId, HttpServletResponse response) throws IOException {
        List<ScoreExcelDTO> excelData = examResultService.getResultsByExam(examId).stream()
                .map(result -> new ScoreExcelDTO(
                        result.getStudent() == null ? "-" : result.getStudent().getUsername(),
                        result.getExam() == null ? "-" : result.getExam().getTitle(),
                        result.getExam() == null || result.getExam().getCourse() == null ? "-" : result.getExam().getCourse().getCourseName(),
                        result.getScore(),
                        formatDateTime(result.getSubmittedAt())
                ))
                .collect(Collectors.toList());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("exam_scores_" + examId, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ScoreExcelDTO.class)
                .sheet("成绩分析")
                .doWrite(excelData);
    }

    @GetMapping("/students/{studentId}/results")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or #studentId == authentication.principal.id")
    public List<ExamResult> getResultsByStudent(@PathVariable Long studentId) {
        return examResultService.getResultsByStudent(studentId);
    }

    @GetMapping("/students/{studentId}/mistakes")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or #studentId == authentication.principal.id")
    public List<StudentAnswer> getMistakesByStudent(@PathVariable Long studentId) {
        return examResultService.getMistakesByStudent(studentId);
    }

    @GetMapping("/results/review/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getExamReview(@PathVariable Long examId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            return ResponseEntity.status(401).body(Map.of("message", "登录状态失效，请重新登录"));
        }
        Long studentId = userDetails.getId();
        ExamResult examResult = examResultRepository.findByExam_IdAndStudent_Id(examId, studentId)
                .orElseThrow(() -> new RuntimeException("未找到该考试成绩记录"));
        List<StudentAnswer> answers = studentAnswerRepository.findByExam_IdAndStudent_Id(examId, studentId);
        boolean hasPendingSubjective = answers.stream().anyMatch(sa -> {
            Question q = sa.getQuestion();
            String type = q == null || q.getType() == null ? "" : q.getType().trim().toUpperCase();
            return "SUBJECTIVE".equals(type) && sa.getScore() == null;
        });
        Double studentScore = hasPendingSubjective ? null : examResult.getScore();
        List<Map<String, Object>> answerList = answers.stream().map(sa -> {
            Question q = sa.getQuestion();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", q == null ? null : q.getId());
            item.put("content", q == null ? "" : String.valueOf(q.getContent() == null ? "" : q.getContent()));
            item.put("type", q == null ? "" : String.valueOf(q.getType() == null ? "" : q.getType()));
            item.put("options", q == null ? "" : String.valueOf(q.getOptions() == null ? "" : q.getOptions()));
            item.put("answer", q == null ? "" : String.valueOf(q.getAnswer() == null ? "" : q.getAnswer()));
            item.put("analysis", null);
            item.put("imageUrl", null);
            item.put("selectedAnswer", sa.getSelectedAnswer() == null ? "" : sa.getSelectedAnswer());
            item.put("score", sa.getScore());
            return item;
        }).collect(Collectors.toList());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("examTitle", examResult.getExam() == null ? "-" : examResult.getExam().getTitle());
        payload.put("totalScore", examResult.getExam() == null ? 0 : examResult.getExam().getTotalScore());
        payload.put("studentScore", studentScore);
        payload.put("examStartTime", examResult.getExam() == null ? null : examResult.getExam().getStartTime());
        payload.put("examEndTime", examResult.getExam() == null ? null : examResult.getExam().getEndTime());
        payload.put("durationInMinutes", examResult.getExam() == null ? null : examResult.getExam().getDurationInMinutes());
        payload.put("submittedAt", examResult.getSubmittedAt());
        payload.put("actualDurationSeconds", examResult.getActualDurationSeconds());
        payload.put("answers", answerList);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/results/pending-grading")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<StudentAnswer> getPendingGrading() {
        return examResultService.getPendingGrading();
    }

    @GetMapping("/results/graded")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<StudentAnswer> getGradedResults() {
        return examResultService.getGradedResults();
    }

    @PostMapping("/results/grade/ai")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> gradeByAi(@RequestBody Map<String, Object> payload) {
        Object answerIdValue = payload.get("studentAnswerId");
        if (answerIdValue == null) {
            return ResponseEntity.badRequest().build();
        }
        Long studentAnswerId = Long.valueOf(String.valueOf(answerIdValue));
        StudentAnswer graded = examResultService.gradeByAi(studentAnswerId);
        Integer finalScore = graded.getScore() == null ? 0 : graded.getScore();
        return ResponseEntity.ok(Collections.singletonMap("score", finalScore));
    }

    @PostMapping("/results/grade/manual")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAnswer> gradeManual(@RequestBody Map<String, Object> payload) {
        Object answerIdValue = payload.get("studentAnswerId");
        if (answerIdValue == null) {
            return ResponseEntity.badRequest().build();
        }
        Long studentAnswerId = Long.valueOf(String.valueOf(answerIdValue));
        Integer score = payload.get("score") == null ? null : Integer.valueOf(String.valueOf(payload.get("score")));
        StudentAnswer graded = examResultService.gradeManual(studentAnswerId, score);
        return ResponseEntity.ok(graded);
    }

    @PostMapping("/ai/tutor/explain")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> explainWrongAnswer(@RequestBody Map<String, Object> payload) {
        Object questionValue = payload.get("question");
        Object standardAnswerValue = payload.get("standardAnswer");
        Object studentAnswerValue = payload.get("studentAnswer");
        if (questionValue == null || standardAnswerValue == null || studentAnswerValue == null) {
            return ResponseEntity.badRequest().build();
        }
        String explanation = aiGradingService.explainWrongAnswer(
                String.valueOf(questionValue),
                String.valueOf(standardAnswerValue),
                String.valueOf(studentAnswerValue)
        );
        return ResponseEntity.ok(Map.of("explanation", explanation));
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) return "-";
        return value.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
    }
}
