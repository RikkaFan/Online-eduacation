package com.example.onlineexam.controller;

import com.example.onlineexam.model.ExamResult;
import com.example.onlineexam.model.StudentAnswer;
import com.example.onlineexam.payload.response.ScoreExcelDTO;
import com.example.onlineexam.security.UserDetailsImpl;
import com.example.onlineexam.service.AiGradingService;
import com.example.onlineexam.service.ExamResultService;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @PostMapping("/exams/{examId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ExamResult> submitAnswers(@PathVariable Long examId, @RequestBody List<StudentAnswer> answers) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long studentId = userDetails.getId();
        ExamResult result = examResultService.submitAnswers(examId, studentId, answers);
        return ResponseEntity.ok(result);
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
                        "-"
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

    @GetMapping("/results/pending-grading")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<StudentAnswer> getPendingGrading() {
        return examResultService.getPendingGrading();
    }

    @PostMapping("/results/grade/ai")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<StudentAnswer> gradeByAi(@RequestBody Map<String, Object> payload) {
        Object answerIdValue = payload.get("studentAnswerId");
        if (answerIdValue == null) {
            return ResponseEntity.badRequest().build();
        }
        Long studentAnswerId = Long.valueOf(String.valueOf(answerIdValue));
        StudentAnswer graded = examResultService.gradeByAi(studentAnswerId);
        return ResponseEntity.ok(graded);
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
}
