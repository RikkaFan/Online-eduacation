package com.example.onlineexam.repository;

import com.example.onlineexam.model.CourseEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseEvaluationRepository extends JpaRepository<CourseEvaluation, Long> {
    boolean existsByCourse_IdAndUserId(Long courseId, Long userId);
    List<CourseEvaluation> findByCourse_IdOrderByCreateTimeDesc(Long courseId);
    long countByCourse_Id(Long courseId);
    List<CourseEvaluation> findByCourse_Id(Long courseId);
}
