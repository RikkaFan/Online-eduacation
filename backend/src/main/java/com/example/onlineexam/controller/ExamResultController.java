package com.example.onlineexam.controller;

import com.example.onlineexam.model.ExamResult;
import com.example.onlineexam.model.StudentAnswer;
import com.example.onlineexam.security.UserDetailsImpl;
import com.example.onlineexam.service.ExamResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ExamResultController {

    @Autowired
    private ExamResultService examResultService;

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

    @GetMapping("/students/{studentId}/results")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or #studentId == authentication.principal.id")
    public List<ExamResult> getResultsByStudent(@PathVariable Long studentId) {
        return examResultService.getResultsByStudent(studentId);
    }
}
