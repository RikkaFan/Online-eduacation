package com.example.onlineexam.repository;

import com.example.onlineexam.model.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    @Query("""
            select sa
            from StudentAnswer sa
            join fetch sa.question q
            where sa.student.id = :studentId
              and (
                sa.selectedAnswer is null
                or sa.selectedAnswer = ''
                or sa.selectedAnswer <> q.answer
              )
            """)
    List<StudentAnswer> findMistakesByStudentId(@Param("studentId") Long studentId);

    @Query("""
            select count(sa)
            from StudentAnswer sa
            join sa.question q
            where sa.student.id = :studentId
              and (
                sa.selectedAnswer is null
                or sa.selectedAnswer = ''
                or sa.selectedAnswer <> q.answer
              )
            """)
    long countMistakesByStudentId(@Param("studentId") Long studentId);

    @Query("""
            select sa
            from StudentAnswer sa
            join fetch sa.exam e
            join fetch sa.student s
            join fetch sa.question q
            where (sa.graded is null or sa.graded = false)
              and (q.type = 'SUBJECTIVE')
            order by sa.id desc
            """)
    List<StudentAnswer> findPendingSubjectiveAnswers();

    List<StudentAnswer> findByExam_IdAndStudent_Id(Long examId, Long studentId);

    void deleteByExam_Id(Long examId);
    void deleteByStudent_Id(Long studentId);
}
