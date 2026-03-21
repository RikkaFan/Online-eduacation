package com.example.onlineexam.service;

import com.example.onlineexam.payload.response.AdminStatsResponse;
import com.example.onlineexam.payload.response.StudentStatsResponse;
import com.example.onlineexam.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatsService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    public AdminStatsResponse getAdminStats(Long currentUserId, boolean teacherOnly) {
        if (teacherOnly && currentUserId != null) {
            List<Long> courseIds = courseRepository.findByTeacherId(currentUserId).stream()
                    .map(course -> course.getId())
                    .toList();
            long totalCourses = courseRepository.countByTeacherId(currentUserId);
            long totalExams = examRepository.countByCourse_TeacherId(currentUserId);
            long totalQuestions = courseIds.isEmpty() ? 0 : questionRepository.countByDeletedFalseAndCourseIdIn(courseIds);
            return new AdminStatsResponse(
                    totalCourses,
                    totalExams,
                    totalQuestions,
                    userRepository.count()
            );
        }
        return new AdminStatsResponse(
                courseRepository.count(),
                examRepository.count(),
                questionRepository.countByDeletedFalse(),
                userRepository.count()
        );
    }

    public StudentStatsResponse getStudentStats(Long studentId) {
        long attendedExams = examResultRepository.countByStudentId(studentId);
        long totalMistakes = studentAnswerRepository.countMistakesByStudentId(studentId);
        Double averageScore = examResultRepository.findAverageScoreByStudentId(studentId);
        return new StudentStatsResponse(
                attendedExams,
                totalMistakes,
                averageScore == null ? 0D : averageScore
        );
    }
}
