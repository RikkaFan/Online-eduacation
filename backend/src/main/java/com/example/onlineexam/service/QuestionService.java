package com.example.onlineexam.service;

import com.example.onlineexam.model.Question;
import com.example.onlineexam.repository.CourseRepository;
import com.example.onlineexam.repository.QuestionRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private CourseRepository courseRepository;

    public List<Question> getAllQuestions() {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            List<Long> courseIds = courseRepository.findByTeacherId(current.getId()).stream()
                    .map(course -> course.getId())
                    .toList();
            if (courseIds.isEmpty()) return List.of();
            return questionRepository.findByCourseIdInAndDeletedFalse(courseIds);
        }
        return questionRepository.findByDeletedFalse();
    }

    public Optional<Question> getQuestionById(Long id) {
        Optional<Question> questionOptional = questionRepository.findByIdAndDeletedFalse(id);
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return questionOptional.filter(question -> ownsCourse(current.getId(), question.getCourseId()));
        }
        return questionOptional;
    }

    public List<Question> getQuestionsByCategoryId(Long categoryId) {
        List<Question> questions = questionRepository.findByCategoryIdAndDeletedFalse(categoryId);
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            return questions.stream()
                    .filter(question -> ownsCourse(current.getId(), question.getCourseId()))
                    .toList();
        }
        return questions;
    }

    public List<Question> getQuestionsByCourseId(Long courseId) {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly() && !ownsCourse(current.getId(), courseId)) {
            throw new AccessDeniedException("无权限访问其他教师课程题库");
        }
        return questionRepository.findByCourseIdAndDeletedFalse(courseId);
    }

    public List<Question> generatePracticeQuestions(Long courseId, int count) {
        List<Question> allQuestions = questionRepository.findByCourseIdAndDeletedFalse(courseId);
        Collections.shuffle(allQuestions);
        int size = Math.min(Math.max(count, 1), allQuestions.size());
        return allQuestions.subList(0, size);
    }

    public Question createQuestion(Question question) {
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly() && !ownsCourse(current.getId(), question.getCourseId())) {
            throw new AccessDeniedException("无权限在其他教师课程下创建题目");
        }
        question.setType(normalizeType(question.getType()));
        question.setDeleted(false);
        return questionRepository.save(question);
    }

    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = questionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly()) {
            if (!ownsCourse(current.getId(), question.getCourseId())) {
                throw new AccessDeniedException("无权限更新其他教师题目");
            }
            Long targetCourseId = questionDetails.getCourseId() == null ? question.getCourseId() : questionDetails.getCourseId();
            if (!ownsCourse(current.getId(), targetCourseId)) {
                throw new AccessDeniedException("无权限迁移题目到其他教师课程");
            }
        }

        question.setContent(questionDetails.getContent());
        question.setOptions(questionDetails.getOptions());
        question.setAnswer(questionDetails.getAnswer());
        question.setType(normalizeType(questionDetails.getType()));
        question.setCategoryId(questionDetails.getCategoryId());
        question.setCourseId(questionDetails.getCourseId());

        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在"));
        UserDetailsImpl current = getCurrentUser();
        if (current != null && isTeacherOnly() && !ownsCourse(current.getId(), question.getCourseId())) {
            throw new AccessDeniedException("无权限删除其他教师题目");
        }
        if (question.isDeleted()) {
            return;
        }
        question.setDeleted(true);
        questionRepository.save(question);
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "SINGLE";
        }
        String normalized = type.trim().toUpperCase();
        if ("SINGLE_CHOICE".equals(normalized)) {
            return "SINGLE";
        }
        if ("TRUE_FALSE".equals(normalized)) {
            return "JUDGE";
        }
        if ("MULTIPLE_CHOICE".equals(normalized)) {
            return "MULTIPLE";
        }
        if ("JUDGE".equals(normalized) || "SUBJECTIVE".equals(normalized)) {
            return normalized;
        }
        if (!"SINGLE".equals(normalized) && !"MULTIPLE".equals(normalized)) {
            return "SINGLE";
        }
        return normalized;
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
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_TEACHER".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private boolean ownsCourse(Long teacherId, Long courseId) {
        if (teacherId == null || courseId == null) return false;
        return courseRepository.existsByIdAndTeacherId(courseId, teacherId);
    }
}
