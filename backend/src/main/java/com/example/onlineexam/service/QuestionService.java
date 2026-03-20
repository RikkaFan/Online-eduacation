package com.example.onlineexam.service;

import com.example.onlineexam.model.Question;
import com.example.onlineexam.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<Question> getAllQuestions() {
        return questionRepository.findByDeletedFalse();
    }

    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findByIdAndDeletedFalse(id);
    }

    public List<Question> getQuestionsByCategoryId(Long categoryId) {
        return questionRepository.findByCategoryIdAndDeletedFalse(categoryId);
    }

    public List<Question> getQuestionsByCourseId(Long courseId) {
        return questionRepository.findByCourseIdAndDeletedFalse(courseId);
    }

    public List<Question> generatePracticeQuestions(Long courseId, int count) {
        List<Question> allQuestions = questionRepository.findByCourseIdAndDeletedFalse(courseId);
        Collections.shuffle(allQuestions);
        int size = Math.min(Math.max(count, 1), allQuestions.size());
        return allQuestions.subList(0, size);
    }

    public Question createQuestion(Question question) {
        question.setType(normalizeType(question.getType()));
        question.setDeleted(false);
        return questionRepository.save(question);
    }

    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = questionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

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
}
