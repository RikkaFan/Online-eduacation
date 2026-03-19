package com.example.onlineexam.controller;

import com.example.onlineexam.model.Chapter;
import com.example.onlineexam.model.Course;
import com.example.onlineexam.repository.ChapterProgressRepository;
import com.example.onlineexam.repository.ChapterRepository;
import com.example.onlineexam.repository.CourseRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ChapterController {

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ChapterProgressRepository chapterProgressRepository;

    @GetMapping("/courses/{courseId}/chapters")
    public List<Chapter> getChaptersByCourse(@PathVariable Long courseId) {
        return chapterRepository.findByCourse_IdOrderBySortOrderAscIdAsc(courseId);
    }

    @PostMapping("/courses/{courseId}/chapters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public Chapter createChapter(@PathVariable Long courseId, @RequestBody CreateChapterRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));

        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setSortOrder(request.getSortOrder());
        return chapterRepository.save(chapter);
    }

    @DeleteMapping("/chapters/{chapterId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> deleteChapter(@PathVariable Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found with id: " + chapterId));
        chapterProgressRepository.deleteByChapterId(chapterId);
        chapterRepository.delete(chapter);
        return ResponseEntity.ok(Map.of("message", "Chapter deleted successfully."));
    }

    @Data
    private static class CreateChapterRequest {
        private String title;
        private String content;
        private Integer sortOrder;
    }
}
