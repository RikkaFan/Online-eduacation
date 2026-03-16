package com.example.onlineexam.service;

import com.example.onlineexam.model.*;
import com.example.onlineexam.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamResultService {

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    public ExamResult submitAnswers(Long examId, Long studentId, List<StudentAnswer> answers) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        int score = 0;
        for (StudentAnswer answer : answers) {
            Question question = questionRepository.findById(answer.getQuestion().getId())
                    .orElseThrow(() -> new RuntimeException("Question not found with id: " + answer.getQuestion().getId()));
            if (question.getCorrectAnswer().equals(answer.getSelectedAnswer())) {
                score++;
            }
            answer.setStudent(student);
            answer.setExam(exam);
            studentAnswerRepository.save(answer);
        }

        ExamResult examResult = new ExamResult();
        examResult.setExam(exam);
        examResult.setStudent(student);
        examResult.setScore(score);

        return examResultRepository.save(examResult);
    }

    public List<ExamResult> getResultsByExam(Long examId) {
        return examResultRepository.findByExamId(examId);
    }

    public List<ExamResult> getResultsByStudent(Long studentId) {
        return examResultRepository.findByStudentId(studentId);
    }
}
