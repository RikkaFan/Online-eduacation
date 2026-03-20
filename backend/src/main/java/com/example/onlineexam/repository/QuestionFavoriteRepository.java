package com.example.onlineexam.repository;

import com.example.onlineexam.model.QuestionFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionFavoriteRepository extends JpaRepository<QuestionFavorite, Long> {
    @Query("select qf from QuestionFavorite qf where qf.userId = :userId order by qf.createTime desc")
    List<QuestionFavorite> findByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId);

    @Query("select qf from QuestionFavorite qf where qf.userId = :userId and qf.question.id = :questionId")
    Optional<QuestionFavorite> findByUserIdAndQuestionId(@Param("userId") Long userId, @Param("questionId") Long questionId);

    @Query("select count(qf) > 0 from QuestionFavorite qf where qf.userId = :userId and qf.question.id = :questionId")
    boolean existsByUserIdAndQuestionId(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
