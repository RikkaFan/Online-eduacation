package com.example.onlineexam.controller;

import com.example.onlineexam.model.Chapter;
import com.example.onlineexam.model.ChapterProgress;
import com.example.onlineexam.repository.ChapterProgressRepository;
import com.example.onlineexam.repository.ChapterRepository;
import com.example.onlineexam.repository.CourseEnrollmentRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class CourseProgressController {

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private ChapterProgressRepository chapterProgressRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @PostMapping("/chapters/{chapterId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public Map<String, Object> completeChapter(@PathVariable Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found with id: " + chapterId));

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        Long courseId = chapter.getCourse() == null ? null : chapter.getCourse().getId();
        boolean enrolled = courseId != null
                && courseEnrollmentRepository.findByCourse_IdAndStudentId(courseId, userId)
                .map(item -> "ENROLLED".equals(item.getStatus()))
                .orElse(false);
        if (!enrolled) {
            throw new org.springframework.security.access.AccessDeniedException("请先选课后再打卡");
        }

        if (!chapterProgressRepository.existsByUserIdAndChapterId(userId, chapter.getId())) {
            ChapterProgress progress = new ChapterProgress();
            progress.setUserId(userId);
            progress.setChapterId(chapter.getId());
            progress.setCompletedAt(LocalDateTime.now());
            chapterProgressRepository.save(progress);
        }

        return Map.of("message", "Chapter completed.");
    }

    @GetMapping("/courses/{courseId}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public Map<String, Object> getCourseProgress(@PathVariable Long courseId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        boolean enrolled = courseEnrollmentRepository.findByCourse_IdAndStudentId(courseId, userId)
                .map(item -> "ENROLLED".equals(item.getStatus()))
                .orElse(false);
        if (!enrolled) {
            return Map.of("total", 0, "completed", 0, "percentage", 0D);
        }

        long total = chapterRepository.countByCourse_Id(courseId);
        long completed = chapterProgressRepository.countCompletedByUserIdAndCourseId(userId, courseId);
        double percentage = total == 0 ? 0D : (completed * 100.0) / total;

        return Map.of(
                "total", total,
                "completed", completed,
                "percentage", percentage
        );
    }

    @GetMapping("/courses/{courseId}/completed-chapters")
    @PreAuthorize("hasRole('STUDENT')")
    public Map<String, Object> getCompletedChapterIds(@PathVariable Long courseId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        boolean enrolled = courseEnrollmentRepository.findByCourse_IdAndStudentId(courseId, userId)
                .map(item -> "ENROLLED".equals(item.getStatus()))
                .orElse(false);
        if (!enrolled) {
            return Map.of("chapterIds", List.of());
        }
        List<Long> chapterIds = chapterProgressRepository.findCompletedChapterIdsByUserIdAndCourseId(userId, courseId);
        return Map.of("chapterIds", chapterIds);
    }
}
