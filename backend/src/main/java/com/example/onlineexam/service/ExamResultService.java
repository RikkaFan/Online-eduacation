package com.example.onlineexam.service;

import com.example.onlineexam.model.*;
import com.example.onlineexam.repository.*;
import com.example.onlineexam.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExamResultService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";

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

    @Transactional
    public ExamResult submitAnswers(Long examId, Long studentId, List<StudentAnswer> answers) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        List<StudentAnswer> safeAnswers = answers == null ? List.of() : answers;
        Set<Long> examQuestionIds = exam.getQuestions() == null
                ? Set.of()
                : exam.getQuestions().stream()
                .map(Question::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        studentAnswerRepository.findByExam_IdAndStudent_Id(examId, studentId).forEach(existing -> studentAnswerRepository.deleteById(existing.getId()));

        int totalScore = 0;
        for (StudentAnswer answer : safeAnswers) {
            Long questionId = answer == null || answer.getQuestion() == null ? null : answer.getQuestion().getId();
            if (questionId == null) {
                throw new IllegalArgumentException("提交失败：存在缺少题目ID的答案");
            }
            if (!examQuestionIds.isEmpty() && !examQuestionIds.contains(questionId)) {
                throw new IllegalArgumentException("提交失败：题目不属于当前试卷，questionId=" + questionId);
            }
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));
            String type = normalizeType(question.getType());
            int maxScore = maxScoreForQuestion(exam, type);

            StudentAnswer persistedAnswer = new StudentAnswer();
            persistedAnswer.setQuestion(question);
            persistedAnswer.setSelectedAnswer(answer.getSelectedAnswer() == null ? "" : answer.getSelectedAnswer().trim());
            if ("SUBJECTIVE".equals(type)) {
                persistedAnswer.setScore(null);
                persistedAnswer.setGraded(false);
            } else {
                String standard = normalizeObjectiveAnswer(question.getAnswer(), type);
                String studentAnswer = normalizeObjectiveAnswer(persistedAnswer.getSelectedAnswer(), type);
                int score = standard.equals(studentAnswer) ? maxScore : 0;
                persistedAnswer.setScore(score);
                persistedAnswer.setGraded(true);
                totalScore += score;
            }
            persistedAnswer.setStudent(student);
            persistedAnswer.setExam(exam);
            studentAnswerRepository.save(persistedAnswer);
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        ExamResult examResult = examResultRepository.findByExam_IdAndStudent_Id(examId, studentId)
                .orElseGet(() -> {
                    ExamResult er = new ExamResult();
                    er.setExam(exam);
                    er.setStudent(student);
                    return er;
                });
        examResult.setScore(totalScore);
        examResult.setStatus(STATUS_PENDING);
        examResult.setSubmittedAt(submittedAt);
        examResult.setActualDurationSeconds(calculateActualDurationSeconds(exam, submittedAt));

        examResultRepository.save(examResult);
        return refreshExamResultScore(examId, studentId);
    }

    public List<ExamResult> getResultsByExam(Long examId) {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return examResultRepository.findByExamIdAndExam_Course_TeacherId(examId, current.getId());
        }
        return examResultRepository.findByExamId(examId);
    }

    public List<ExamResult> getResultsByStudent(Long studentId) {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return examResultRepository.findByStudentIdAndExam_Course_TeacherId(studentId, current.getId());
        }
        return examResultRepository.findByStudentId(studentId);
    }

    public List<StudentAnswer> getMistakesByStudent(Long studentId) {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return studentAnswerRepository.findMistakesByStudentIdAndTeacherId(studentId, current.getId());
        }
        return studentAnswerRepository.findMistakesByStudentId(studentId);
    }

    public List<StudentAnswer> getPendingGrading() {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return studentAnswerRepository.findPendingSubjectiveAnswersByTeacherId(current.getId());
        }
        return studentAnswerRepository.findPendingSubjectiveAnswers();
    }

    public List<StudentAnswer> getGradedResults() {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return studentAnswerRepository.findFullyGradedSubjectiveAnswersByTeacherId(current.getId());
        }
        return studentAnswerRepository.findFullyGradedSubjectiveAnswers();
    }

    public StudentAnswer gradeByAi(Long studentAnswerId) {
        StudentAnswer answer = studentAnswerRepository.findById(studentAnswerId)
                .orElseThrow(() -> new RuntimeException("StudentAnswer not found with id: " + studentAnswerId));
        enforceTeacherOwnership(answer);
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
        enforceTeacherOwnership(answer);
        int maxScore = maxScoreForQuestion(answer.getExam(), normalizeType(answer.getQuestion().getType()));
        int safeScore = score == null ? 0 : Math.max(0, Math.min(score, maxScore));
        answer.setScore(safeScore);
        answer.setGraded(true);
        StudentAnswer saved = studentAnswerRepository.save(answer);
        refreshExamResultScore(saved.getExam().getId(), saved.getStudent().getId());
        return saved;
    }

    private ExamResult refreshExamResultScore(Long examId, Long studentId) {
        List<StudentAnswer> allAnswers = studentAnswerRepository.findByExam_IdAndStudent_Id(examId, studentId);
        int total = allAnswers.stream()
                .map(StudentAnswer::getScore)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        boolean hasSubjective = allAnswers.stream()
                .anyMatch(sa -> "SUBJECTIVE".equals(normalizeType(sa.getQuestion() == null ? null : sa.getQuestion().getType())));
        boolean allSubjectiveGraded = allAnswers.stream()
                .filter(sa -> "SUBJECTIVE".equals(normalizeType(sa.getQuestion() == null ? null : sa.getQuestion().getType())))
                .allMatch(sa -> sa.getScore() != null);
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
        examResult.setStatus(!hasSubjective || allSubjectiveGraded ? STATUS_COMPLETED : STATUS_PENDING);
        return examResultRepository.save(examResult);
    }

    private Integer calculateActualDurationSeconds(Exam exam, LocalDateTime submittedAt) {
        if (exam == null || submittedAt == null || exam.getStartTime() == null) return null;
        Duration duration = Duration.between(exam.getStartTime(), submittedAt);
        if (duration.isNegative()) return 0;
        long seconds = duration.getSeconds();
        if (seconds > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) seconds;
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

    private void enforceTeacherOwnership(StudentAnswer answer) {
        UserDetailsImpl current = getCurrentUser();
        if (current == null || !isTeacherOnly()) {
            return;
        }
        Long ownerId = answer.getExam() == null || answer.getExam().getCourse() == null
                ? null
                : answer.getExam().getCourse().getTeacherId();
        if (ownerId == null || !ownerId.equals(current.getId())) {
            throw new AccessDeniedException("无权限批改其他教师试卷");
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
