package com.example.onlineexam.service;

import com.example.onlineexam.model.Course;
import com.example.onlineexam.repository.CourseRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    public Course createCourse(Course course) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl user) {
            System.out.println("====== Extracted Teacher ID from SecurityContext: " + user.getId() + " ======");
            System.out.println("====== Incoming Course.teacherId: " + course.getTeacherId() + " ======");
            if (course.getTeacherId() == null) {
                course.setTeacherId(user.getId());
                System.out.println("====== teacherId was null, set to current user id: " + course.getTeacherId() + " ======");
            } else {
                System.out.println("====== teacherId provided by client will be respected: " + course.getTeacherId() + " ======");
            }
        } else {
            System.out.println("====== SecurityContext principal is null or not UserDetailsImpl ======");
        }
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, Course courseDetails) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        
        course.setCourseName(courseDetails.getCourseName());
        course.setDescription(courseDetails.getDescription());
        course.setTeacherId(courseDetails.getTeacherId());

        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
        // 再次校验，确保数据库层面确实删除成功
        if (courseRepository.existsById(id)) {
            throw new RuntimeException("Failed to delete course with id: " + id);
        }
    }
}
