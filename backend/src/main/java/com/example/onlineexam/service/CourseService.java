package com.example.onlineexam.service;

import com.example.onlineexam.model.Course;
import com.example.onlineexam.model.CourseEnrollment;
import com.example.onlineexam.repository.CourseEnrollmentRepository;
import com.example.onlineexam.repository.CourseRepository;
import com.example.onlineexam.repository.ExamRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired
    private ExamRepository examRepository;

    public List<Course> getAllCourses() {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return courseRepository.findByTeacherId(current.getId());
        }
        return courseRepository.findAll();
    }

    public Optional<Course> getCourseById(Long id) {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return courseRepository.findById(id)
                    .filter(course -> current.getId().equals(course.getTeacherId()));
        }
        return courseRepository.findById(id);
    }

    public List<Course> getEnrolledCourses() {
        UserDetailsImpl current = getCurrentUser();
        if (current == null) return List.of();
        if (!isStudentOnly()) return List.of();
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentIdAndStatusOrderByUpdateTimeDesc(current.getId(), "ENROLLED");
        Set<Long> courseIds = enrollments.stream()
                .map(item -> item.getCourse() == null ? null : item.getCourse().getId())
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        if (courseIds.isEmpty()) return List.of();
        return courseRepository.findAllById(courseIds);
    }

    public void enrollCourse(Long courseId) {
        UserDetailsImpl current = getCurrentUser();
        if (current == null || !isStudentOnly()) {
            throw new AccessDeniedException("仅学生可选课");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        CourseEnrollment enrollment = courseEnrollmentRepository.findByCourse_IdAndStudentId(course.getId(), current.getId())
                .orElseGet(() -> {
                    CourseEnrollment created = new CourseEnrollment();
                    created.setCourse(course);
                    created.setStudentId(current.getId());
                    created.setCreateTime(java.time.LocalDateTime.now());
                    return created;
                });
        enrollment.setStatus("ENROLLED");
        enrollment.setUpdateTime(java.time.LocalDateTime.now());
        if (enrollment.getCreateTime() == null) {
            enrollment.setCreateTime(java.time.LocalDateTime.now());
        }
        courseEnrollmentRepository.save(enrollment);
    }

    public void unenrollCourse(Long courseId) {
        UserDetailsImpl current = getCurrentUser();
        if (current == null || !isStudentOnly()) {
            throw new AccessDeniedException("仅学生可退课");
        }
        CourseEnrollment enrollment = courseEnrollmentRepository.findByCourse_IdAndStudentId(courseId, current.getId())
                .orElseThrow(() -> new RuntimeException("未找到选课记录"));
        enrollment.setStatus("UNENROLLED");
        enrollment.setUpdateTime(java.time.LocalDateTime.now());
        if (enrollment.getCreateTime() == null) {
            enrollment.setCreateTime(java.time.LocalDateTime.now());
        }
        courseEnrollmentRepository.save(enrollment);
    }

    public Course createCourse(Course course) {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            course.setTeacherId(current.getId());
        }
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, Course courseDetails) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly() && !current.getId().equals(course.getTeacherId())) {
            throw new AccessDeniedException("无权限修改其他教师课程");
        }
        
        course.setCourseName(courseDetails.getCourseName());
        course.setDescription(courseDetails.getDescription());
        if (current != null && isTeacherOnly()) {
            course.setTeacherId(current.getId());
        } else {
            course.setTeacherId(courseDetails.getTeacherId());
        }

        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found with id: " + id);
        }
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly() && !courseRepository.existsByIdAndTeacherId(id, current.getId())) {
            throw new AccessDeniedException("无权限删除其他教师课程");
        }
        // 删除课程前，清理关联考试，避免外键约束阻止删除
        examRepository.deleteByCourse_Id(id);
        courseEnrollmentRepository.deleteByCourse_Id(id);
        courseRepository.deleteById(id);
        // 再次校验，确保数据库层面确实删除成功
        if (courseRepository.existsById(id)) {
            throw new RuntimeException("Failed to delete course with id: " + id);
        }
    }

    private UserDetailsImpl getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl user) {
            return user;
        }
        return null;
    }

    private boolean isTeacherOnly() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_TEACHER".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private boolean isStudentOnly() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_STUDENT".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
