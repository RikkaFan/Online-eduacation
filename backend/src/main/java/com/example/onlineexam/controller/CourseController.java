package com.example.onlineexam.controller;

import com.example.onlineexam.annotation.LogAction;
import com.example.onlineexam.model.Course;
import com.example.onlineexam.service.CourseService;
import com.example.onlineexam.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
        return ResponseEntity.ok(course);
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("创建了新课程")
    public Course createCourse(@RequestBody Course course, @AuthenticationPrincipal UserDetailsImpl user) {
        if (user != null) {
            System.out.println("====== Extracted Teacher ID from @AuthenticationPrincipal: " + user.getId() + " ======");
            if (course.getTeacherId() == null) {
                course.setTeacherId(user.getId());
                System.out.println("====== Set Course.teacherId in controller to: " + course.getTeacherId() + " ======");
            }
        } else {
            System.out.println("====== @AuthenticationPrincipal is null in controller ======");
        }
        return courseService.createCourse(course);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course courseDetails) {
        Course updatedCourse = courseService.updateCourse(id, courseDetails);
        return ResponseEntity.ok(updatedCourse);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("删除了课程")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
