package com.example.onlineexam.repository;

import com.example.onlineexam.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCourse_Id(Long courseId);
    void deleteByCourse_Id(Long courseId);

    @Modifying
    @Query(value = "delete from exam_questions where exam_id = :examId", nativeQuery = true)
    void deleteExamQuestionsByExamId(@Param("examId") Long examId);
}
