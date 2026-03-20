package com.example.onlineexam.repository;

import com.example.onlineexam.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByDeletedFalse();
    List<Question> findByCategoryIdAndDeletedFalse(Long categoryId);
    List<Question> findByCourseIdAndDeletedFalse(Long courseId);
    Optional<Question> findByIdAndDeletedFalse(Long id);
    List<Question> findAllByIdInAndDeletedFalse(Collection<Long> ids);
    long countByDeletedFalse();
}
