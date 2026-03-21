package com.example.onlineexam.service;

import com.example.onlineexam.model.Course;
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

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;
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
        boolean teacher = false;
        boolean admin = false;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_TEACHER".equals(authority.getAuthority())) {
                teacher = true;
            }
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                admin = true;
            }
        }
        return teacher && !admin;
    }
}
