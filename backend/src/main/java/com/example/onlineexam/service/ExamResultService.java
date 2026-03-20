package com.example.onlineexam.service;

import com.example.onlineexam.model.*;
import com.example.onlineexam.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

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

    @Autowired
    private AiGradingService aiGradingService;

    public ExamResult submitAnswers(Long examId, Long studentId, List<StudentAnswer> answers) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        int totalScore = 0;
        for (StudentAnswer answer : answers) {
            Question question = questionRepository.findById(answer.getQuestion().getId())
                    .orElseThrow(() -> new RuntimeException("Question not found with id: " + answer.getQuestion().getId()));
            String type = normalizeType(question.getType());
            int maxScore = maxScoreForQuestion(exam, type);

            answer.setQuestion(question);
            answer.setSelectedAnswer(answer.getSelectedAnswer() == null ? "" : answer.getSelectedAnswer().trim());
            if ("SUBJECTIVE".equals(type)) {
                answer.setScore(null);
                answer.setGraded(false);
            } else {
                String standard = normalizeObjectiveAnswer(question.getAnswer(), type);
                String studentAnswer = normalizeObjectiveAnswer(answer.getSelectedAnswer(), type);
                int score = standard.equals(studentAnswer) ? maxScore : 0;
                answer.setScore(score);
                answer.setGraded(true);
                totalScore += score;
            }
            answer.setStudent(student);
            answer.setExam(exam);
            studentAnswerRepository.save(answer);
        }

        ExamResult examResult = new ExamResult();
        examResult.setExam(exam);
        examResult.setStudent(student);
        examResult.setScore(totalScore);

        return examResultRepository.save(examResult);
    }

    public List<ExamResult> getResultsByExam(Long examId) {
        return examResultRepository.findByExamId(examId);
    }

    public List<ExamResult> getResultsByStudent(Long studentId) {
        return examResultRepository.findByStudentId(studentId);
    }

    public List<StudentAnswer> getMistakesByStudent(Long studentId) {
        return studentAnswerRepository.findMistakesByStudentId(studentId);
    }

    public List<StudentAnswer> getPendingGrading() {
        return studentAnswerRepository.findPendingSubjectiveAnswers();
    }

    public StudentAnswer gradeByAi(Long studentAnswerId) {
        StudentAnswer answer = studentAnswerRepository.findById(studentAnswerId)
                .orElseThrow(() -> new RuntimeException("StudentAnswer not found with id: " + studentAnswerId));
        String type = normalizeType(answer.getQuestion().getType());
        if (!"SUBJECTIVE".equals(type)) {
            throw new RuntimeException("该题不是主观题，无需 AI 批改");
        }
        int maxScore = maxScoreForQuestion(answer.getExam(), type);
        Integer score = aiGradingService.gradeSubjectiveQuestion(
                answer.getQuestion().getContent(),
                answer.getQuestion().getAnswer(),
                answer.getSelectedAnswer(),
                maxScore
        );
        answer.setScore(score);
        answer.setGraded(true);
        StudentAnswer saved = studentAnswerRepository.save(answer);
        refreshExamResultScore(saved.getExam().getId(), saved.getStudent().getId());
        return saved;
    }

    public StudentAnswer gradeManual(Long studentAnswerId, Integer score) {
        StudentAnswer answer = studentAnswerRepository.findById(studentAnswerId)
                .orElseThrow(() -> new RuntimeException("StudentAnswer not found with id: " + studentAnswerId));
        int maxScore = maxScoreForQuestion(answer.getExam(), normalizeType(answer.getQuestion().getType()));
        int safeScore = score == null ? 0 : Math.max(0, Math.min(score, maxScore));
        answer.setScore(safeScore);
        answer.setGraded(true);
        StudentAnswer saved = studentAnswerRepository.save(answer);
        refreshExamResultScore(saved.getExam().getId(), saved.getStudent().getId());
        return saved;
    }

    private void refreshExamResultScore(Long examId, Long studentId) {
        List<StudentAnswer> allAnswers = studentAnswerRepository.findByExam_IdAndStudent_Id(examId, studentId);
        int total = allAnswers.stream()
                .map(StudentAnswer::getScore)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        ExamResult examResult = examResultRepository.findByExam_IdAndStudent_Id(examId, studentId)
                .orElseGet(() -> {
                    Exam exam = examRepository.findById(examId).orElseThrow();
                    User student = userRepository.findById(studentId).orElseThrow();
                    ExamResult er = new ExamResult();
                    er.setExam(exam);
                    er.setStudent(student);
                    return er;
                });
        examResult.setScore(total);
        examResultRepository.save(examResult);
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) return "SINGLE";
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MULTIPLE", "MULTIPLE_CHOICE" -> "MULTIPLE";
            case "JUDGE", "TRUE_FALSE" -> "JUDGE";
            case "SUBJECTIVE" -> "SUBJECTIVE";
            default -> "SINGLE";
        };
    }

    private String normalizeObjectiveAnswer(String answer, String type) {
        String normalized = answer == null ? "" : answer.trim().toUpperCase(Locale.ROOT);
        if ("MULTIPLE".equals(type)) {
            return java.util.Arrays.stream(normalized.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(","));
        }
        return normalized;
    }

    private int maxScoreForQuestion(Exam exam, String type) {
        if (exam == null) return 1;
        return switch (type) {
            case "MULTIPLE" -> safeScore(exam.getMultipleScore());
            case "JUDGE" -> safeScore(exam.getJudgeScore());
            case "SUBJECTIVE" -> safeScore(exam.getSubjectiveScore());
            default -> safeScore(exam.getSingleScore());
        };
    }

    private int safeScore(Integer value) {
        return value == null || value < 0 ? 1 : value;
    }
}
