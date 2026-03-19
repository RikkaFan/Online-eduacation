package com.example.onlineexam.repository;

import com.example.onlineexam.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByCourse_IdOrderBySortOrderAscIdAsc(Long courseId);
    long countByCourse_Id(Long courseId);
}
