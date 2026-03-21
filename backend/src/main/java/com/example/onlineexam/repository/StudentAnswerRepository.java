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

    @Query("""
            select sa
            from StudentAnswer sa
            join fetch sa.exam e
            join fetch sa.student s
            join fetch sa.question q
            where (sa.graded is null or sa.graded = false)
              and q.type = 'SUBJECTIVE'
              and e.course.teacherId = :teacherId
            order by sa.id desc
            """)
    List<StudentAnswer> findPendingSubjectiveAnswersByTeacherId(@Param("teacherId") Long teacherId);

    @Query("""
            select sa
            from StudentAnswer sa
            join fetch sa.exam e
            join fetch sa.student s
            join fetch sa.question q
            where q.type = 'SUBJECTIVE'
              and exists (
                select 1
                from StudentAnswer sx
                join sx.question qx
                where sx.exam.id = sa.exam.id
                  and sx.student.id = sa.student.id
                  and qx.type = 'SUBJECTIVE'
              )
              and not exists (
                select 1
                from StudentAnswer sy
                join sy.question qy
                where sy.exam.id = sa.exam.id
                  and sy.student.id = sa.student.id
                  and qy.type = 'SUBJECTIVE'
                  and sy.score is null
              )
            order by sa.id desc
            """)
    List<StudentAnswer> findFullyGradedSubjectiveAnswers();

    @Query("""
            select sa
            from StudentAnswer sa
            join fetch sa.exam e
            join fetch sa.student s
            join fetch sa.question q
            where q.type = 'SUBJECTIVE'
              and e.course.teacherId = :teacherId
              and exists (
                select 1
                from StudentAnswer sx
                join sx.question qx
                where sx.exam.id = sa.exam.id
                  and sx.student.id = sa.student.id
                  and qx.type = 'SUBJECTIVE'
              )
              and not exists (
                select 1
                from StudentAnswer sy
                join sy.question qy
                where sy.exam.id = sa.exam.id
                  and sy.student.id = sa.student.id
                  and qy.type = 'SUBJECTIVE'
                  and sy.score is null
              )
            order by sa.id desc
            """)
    List<StudentAnswer> findFullyGradedSubjectiveAnswersByTeacherId(@Param("teacherId") Long teacherId);

    @Query("""
            select sa
            from StudentAnswer sa
            join fetch sa.question q
            join sa.exam e
            where sa.student.id = :studentId
              and e.course.teacherId = :teacherId
              and (
                sa.selectedAnswer is null
                or sa.selectedAnswer = ''
                or sa.selectedAnswer <> q.answer
              )
            """)
    List<StudentAnswer> findMistakesByStudentIdAndTeacherId(@Param("studentId") Long studentId, @Param("teacherId") Long teacherId);

    List<StudentAnswer> findByExam_IdAndStudent_Id(Long examId, Long studentId);

    long countByQuestion_Id(Long questionId);
    void deleteByExam_Id(Long examId);
    void deleteByStudent_Id(Long studentId);
}
