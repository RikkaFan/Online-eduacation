package com.example.onlineexam.repository;

import com.example.onlineexam.model.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByExamId(Long examId);
    List<ExamResult> findByStudentId(Long studentId);
    void deleteByExamId(Long examId);
    long countByStudentId(Long studentId);

    @Query("select avg(er.score) from ExamResult er where er.student.id = :studentId")
    Double findAverageScoreByStudentId(@Param("studentId") Long studentId);
}
