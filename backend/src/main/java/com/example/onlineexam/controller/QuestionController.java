package com.example.onlineexam.controller;

import com.example.onlineexam.model.Question;
import com.example.onlineexam.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @GetMapping("/courses/{courseId}/questions")
    public List<Question> getQuestionsByCourse(@PathVariable Long courseId) {
        return questionService.getQuestionsByCourse(courseId);
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        return ResponseEntity.ok(question);
    }

    @PostMapping("/courses/{courseId}/questions")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Question createQuestion(@PathVariable Long courseId, @RequestBody Question question) {
        return questionService.createQuestion(courseId, question);
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question questionDetails) {
        Question updatedQuestion = questionService.updateQuestion(id, questionDetails);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
