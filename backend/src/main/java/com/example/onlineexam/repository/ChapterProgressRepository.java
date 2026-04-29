package com.example.onlineexam.repository;

import com.example.onlineexam.model.ChapterProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChapterProgressRepository extends JpaRepository<ChapterProgress, Long> {
    boolean existsByUserIdAndChapterId(Long userId, Long chapterId);
    void deleteByChapterId(Long chapterId);

    @Query("""
            select count(cp)
            from ChapterProgress cp
            where cp.userId = :userId
              and cp.chapterId in (
                select c.id
                from Chapter c
                where c.course.id = :courseId
              )
            """)
    long countCompletedByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("""
            select cp.chapterId
            from ChapterProgress cp
            where cp.userId = :userId
              and cp.chapterId in (
                select c.id
                from Chapter c
                where c.course.id = :courseId
              )
            """)
    List<Long> findCompletedChapterIdsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
