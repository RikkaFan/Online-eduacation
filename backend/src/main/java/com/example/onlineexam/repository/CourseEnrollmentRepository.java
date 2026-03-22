package com.example.onlineexam.repository;

import com.example.onlineexam.model.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    Optional<CourseEnrollment> findByCourse_IdAndStudentId(Long courseId, Long studentId);
    List<CourseEnrollment> findByStudentIdAndStatusOrderByUpdateTimeDesc(Long studentId, String status);
    List<CourseEnrollment> findByCourse_IdAndStatusOrderByUpdateTimeDesc(Long courseId, String status);
    void deleteByCourse_Id(Long courseId);
}
