package com.example.onlineexam.controller;

import com.example.onlineexam.model.Exam;
import com.example.onlineexam.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ExamController {

    @Autowired
    private ExamService examService;

    @GetMapping("/courses/{courseId}/exams")
    public List<Exam> getExamsByCourse(@PathVariable Long courseId) {
        return examService.getExamsByCourse(courseId);
    }

    @GetMapping("/exams/{id}")
    public ResponseEntity<Exam> getExamById(@PathVariable Long id) {
        Exam exam = examService.getExamById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));
        return ResponseEntity.ok(exam);
    }

    @PostMapping("/courses/{courseId}/exams")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Exam createExam(@PathVariable Long courseId, @RequestBody Exam exam, @RequestParam int numberOfQuestions) {
        return examService.createExam(courseId, exam, numberOfQuestions);
    }

    @PutMapping("/exams/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Exam> updateExam(@PathVariable Long id, @RequestBody Exam examDetails) {
        Exam updatedExam = examService.updateExam(id, examDetails);
        return ResponseEntity.ok(updatedExam);
    }

    @DeleteMapping("/exams/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
}
