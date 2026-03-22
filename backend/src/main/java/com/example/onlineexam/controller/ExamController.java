package com.example.onlineexam.controller;

import com.example.onlineexam.annotation.LogAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.onlineexam.model.Exam;
import com.example.onlineexam.model.Question;
import com.example.onlineexam.payload.response.StudentExamDTO;
import com.example.onlineexam.payload.response.StudentQuestionDTO;
import com.example.onlineexam.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/courses/{courseId}/exams")
    public List<Exam> getExamsByCourse(@PathVariable Long courseId) {
        return examService.getExamsByCourse(courseId);
    }

    @GetMapping("/students/exams")
    @PreAuthorize("hasRole('STUDENT')")
    public List<Exam> getEnrolledExamsForStudent() {
        return examService.getEnrolledExamsForStudent();
    }

    @GetMapping("/exams/{id}")
    public ResponseEntity<?> getExamById(@PathVariable Long id) {
        Exam exam = examService.getExamById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));
        exam.setQuestions(sortQuestions(exam.getQuestions()));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && isStudentOnly(authentication)) {
            return ResponseEntity.ok(toStudentExamDTO(exam));
        }
        return ResponseEntity.ok(exam);
    }

    @GetMapping("/exams/{id}/questions")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<Question> getExamQuestions(@PathVariable Long id) {
        Exam exam = examService.getExamById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));
        return sortQuestions(exam.getQuestions());
    }

    @PostMapping("/courses/{courseId}/exams")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("发布了新考试")
    public Exam createExam(@PathVariable Long courseId, @RequestBody Map<String, Object> payload, @RequestParam(defaultValue = "0") int numberOfQuestions) {
        Exam exam = objectMapper.convertValue(payload, Exam.class);
        List<Integer> rawIds = (List<Integer>) payload.get("questionIds");
        List<Long> questionIds = rawIds != null ? rawIds.stream().map(Integer::longValue).collect(Collectors.toList()) : null;
        if (questionIds == null && payload.get("questionIds") instanceof List<?> genericIds) {
            questionIds = genericIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(v -> Long.valueOf(String.valueOf(v)))
                    .collect(Collectors.toList());
        }
        return examService.createExam(courseId, exam, numberOfQuestions, questionIds);
    }

    @PutMapping("/exams/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("更新了考试")
    public ResponseEntity<Exam> updateExam(@PathVariable Long id, @RequestBody Exam examDetails) {
        Exam updatedExam = examService.updateExam(id, examDetails);
        return ResponseEntity.ok(updatedExam);
    }

    @DeleteMapping("/exams/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("删除了考试")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isStudentOnly(Authentication authentication) {
        boolean hasStudent = false;
        boolean hasTeacherOrAdmin = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_STUDENT".equals(role)) {
                hasStudent = true;
            }
            if ("ROLE_TEACHER".equals(role) || "ROLE_ADMIN".equals(role)) {
                hasTeacherOrAdmin = true;
            }
        }
        return hasStudent && !hasTeacherOrAdmin;
    }

    private StudentExamDTO toStudentExamDTO(Exam exam) {
        List<StudentQuestionDTO> questions = sortQuestions(exam.getQuestions()).stream()
                .map(this::toStudentQuestionDTO)
                .collect(Collectors.toList());
        return new StudentExamDTO(
                exam.getId(),
                exam.getCourse(),
                exam.getTitle(),
                exam.getDurationInMinutes(),
                exam.getStartTime(),
                exam.getEndTime(),
                exam.getSingleCount(),
                exam.getSingleScore(),
                exam.getMultipleCount(),
                exam.getMultipleScore(),
                exam.getJudgeCount(),
                exam.getJudgeScore(),
                exam.getSubjectiveCount(),
                exam.getSubjectiveScore(),
                exam.getTotalScore(),
                questions
        );
    }

    private List<Question> sortQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        return questions.stream()
                .sorted(Comparator.comparingInt((Question q) -> typeOrder(q.getType()))
                        .thenComparing(q -> q.getId() == null ? Long.MAX_VALUE : q.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private int typeOrder(String type) {
        return switch (normalizeType(type)) {
            case "MULTIPLE" -> 2;
            case "JUDGE" -> 3;
            case "SUBJECTIVE" -> 4;
            default -> 1;
        };
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "SINGLE";
        }
        String normalized = type.trim().toUpperCase();
        return switch (normalized) {
            case "MULTIPLE", "MULTIPLE_CHOICE" -> "MULTIPLE";
            case "JUDGE", "TRUE_FALSE" -> "JUDGE";
            case "SUBJECTIVE" -> "SUBJECTIVE";
            default -> "SINGLE";
        };
    }

    private StudentQuestionDTO toStudentQuestionDTO(Question question) {
        return new StudentQuestionDTO(
                question.getId(),
                question.getCategoryId(),
                question.getContent(),
                question.getType(),
                question.getOptions(),
                null,
                null
        );
    }
}
