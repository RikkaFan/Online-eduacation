package com.example.onlineexam.controller;

import com.example.onlineexam.model.Course;
import com.example.onlineexam.model.CourseEvaluation;
import com.example.onlineexam.repository.CourseEvaluationRepository;
import com.example.onlineexam.repository.CourseRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class CourseEvaluationController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEvaluationRepository courseEvaluationRepository;

    @PostMapping("/courses/{courseId}/evaluations")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createEvaluation(@PathVariable Long courseId, @RequestBody CreateEvaluationRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));

        if (courseEvaluationRepository.existsByCourse_IdAndUserId(courseId, userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "你已评价过该课程"));
        }

        Integer rating = request.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(Map.of("message", "评分必须在 1 到 5 之间"));
        }

        CourseEvaluation evaluation = new CourseEvaluation();
        evaluation.setCourse(course);
        evaluation.setUserId(userId);
        evaluation.setRating(rating);
        evaluation.setComment(request.getComment());
        CourseEvaluation saved = courseEvaluationRepository.save(evaluation);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/courses/{courseId}/evaluations")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public List<CourseEvaluation> getEvaluations(@PathVariable Long courseId) {
        return courseEvaluationRepository.findByCourse_IdOrderByCreateTimeDesc(courseId);
    }

    @GetMapping("/courses/{courseId}/evaluations/stats")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public Map<String, Object> getEvaluationStats(@PathVariable Long courseId) {
        List<CourseEvaluation> evaluations = courseEvaluationRepository.findByCourse_Id(courseId);
        long total = evaluations.size();
        double average = total == 0
                ? 0D
                : evaluations.stream().mapToInt(e -> e.getRating() == null ? 0 : e.getRating()).average().orElse(0D);
        double averageOneDecimal = Math.round(average * 10.0) / 10.0;

        return Map.of(
                "totalElements", total,
                "averageRating", averageOneDecimal
        );
    }

    @Data
    private static class CreateEvaluationRequest {
        private Integer rating;
        private String comment;
    }
}
