package com.example.onlineexam.controller;

import com.example.onlineexam.model.Chapter;
import com.example.onlineexam.model.Course;
import com.example.onlineexam.repository.ChapterProgressRepository;
import com.example.onlineexam.repository.ChapterRepository;
import com.example.onlineexam.repository.CourseEnrollmentRepository;
import com.example.onlineexam.repository.CourseRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
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

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @GetMapping("/courses/{courseId}/chapters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public List<Chapter> getChaptersByCourse(@PathVariable Long courseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl user) {
            boolean student = auth.getAuthorities().stream().anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
            if (student) {
                boolean enrolled = courseEnrollmentRepository.findByCourse_IdAndStudentId(courseId, user.getId())
                        .map(item -> "ENROLLED".equals(item.getStatus()))
                        .orElse(false);
                if (!enrolled) {
                    throw new org.springframework.security.access.AccessDeniedException("请先选课后查看课时");
                }
            }
        }
        return chapterRepository.findByCourse_IdOrderBySortOrderAscIdAsc(courseId);
    }

    @PostMapping("/courses/{courseId}/chapters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public Chapter createChapter(@PathVariable Long courseId, @RequestBody CreateChapterRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        enforceTeacherCourseOwnership(course);

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
        enforceTeacherCourseOwnership(chapter.getCourse());
        chapterProgressRepository.deleteByChapterId(chapterId);
        chapterRepository.delete(chapter);
        return ResponseEntity.ok(Map.of("message", "Chapter deleted successfully."));
    }

    @PostMapping(value = "/chapters/{chapterId}/material", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> uploadChapterMaterial(@PathVariable Long chapterId, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "上传文件不能为空"));
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("message", "文件不能超过10MB"));
        }
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found with id: " + chapterId));
        enforceTeacherCourseOwnership(chapter.getCourse());
        try {
            chapter.setMaterialName(file.getOriginalFilename());
            chapter.setMaterialContentType(file.getContentType());
            chapter.setMaterialSize(file.getSize());
            chapter.setMaterialData(file.getBytes());
            chapterRepository.save(chapter);
            return ResponseEntity.ok(Map.of(
                    "message", "资料上传成功",
                    "materialName", chapter.getMaterialName(),
                    "materialSize", chapter.getMaterialSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "资料上传失败"));
        }
    }

    @GetMapping("/chapters/{chapterId}/material")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<byte[]> downloadChapterMaterial(@PathVariable Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found with id: " + chapterId));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl user) {
            boolean student = auth.getAuthorities().stream().anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
            if (student) {
                Long courseId = chapter.getCourse() == null ? null : chapter.getCourse().getId();
                boolean enrolled = courseId != null
                        && courseEnrollmentRepository.findByCourse_IdAndStudentId(courseId, user.getId())
                        .map(item -> "ENROLLED".equals(item.getStatus()))
                        .orElse(false);
                if (!enrolled) {
                    throw new org.springframework.security.access.AccessDeniedException("请先选课后再下载资料");
                }
            }
        }
        if (chapter.getMaterialData() == null || chapter.getMaterialData().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该课时暂无资料");
        }
        String filename = chapter.getMaterialName() == null ? "material.bin" : chapter.getMaterialName();
        String contentType = chapter.getMaterialContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : chapter.getMaterialContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType(contentType))
                .body(chapter.getMaterialData());
    }

    private void enforceTeacherCourseOwnership(Course course) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl user)) {
            return;
        }
        boolean teacher = auth.getAuthorities().stream().anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
        if (teacher) {
            Long ownerId = course == null ? null : course.getTeacherId();
            if (ownerId == null || !ownerId.equals(user.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("无权限操作其他教师课程课时");
            }
        }
    }

    @Data
    private static class CreateChapterRequest {
        private String title;
        private String content;
        private Integer sortOrder;
    }
}
