package com.example.onlineexam.service;

import com.example.onlineexam.payload.response.AdminStatsResponse;
import com.example.onlineexam.payload.response.StudentStatsResponse;
import com.example.onlineexam.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public AdminStatsResponse getAdminStats() {
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
