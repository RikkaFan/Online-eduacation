package com.example.onlineexam.repository;

import com.example.onlineexam.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCourse_Id(Long courseId);
    List<Exam> findByCourse_IdAndCourse_TeacherId(Long courseId, Long teacherId);
    boolean existsByIdAndCourse_TeacherId(Long examId, Long teacherId);
    long countByCourse_TeacherId(Long teacherId);
    void deleteByCourse_Id(Long courseId);

    @Modifying
    @Query(value = "delete from exam_questions where exam_id = :examId", nativeQuery = true)
    void deleteExamQuestionsByExamId(@Param("examId") Long examId);

    @Modifying
    @Query(value = "delete from exam_questions where question_id = :questionId", nativeQuery = true)
    void deleteExamQuestionsByQuestionId(@Param("questionId") Long questionId);
}
