package com.example.onlineexam.service;

import com.example.onlineexam.model.Question;
import com.example.onlineexam.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }

    public List<Question> getQuestionsByCategoryId(Long categoryId) {
        return questionRepository.findByCategoryId(categoryId);
    }

    public List<Question> getQuestionsByCourseId(Long courseId) {
        return questionRepository.findByCourseId(courseId);
    }

    public List<Question> generatePracticeQuestions(Long courseId, int count) {
        List<Question> allQuestions = questionRepository.findByCourseId(courseId);
        Collections.shuffle(allQuestions);
        int size = Math.min(Math.max(count, 1), allQuestions.size());
        return allQuestions.subList(0, size);
    }

    public Question createQuestion(Question question) {
        question.setType(normalizeType(question.getType()));
        return questionRepository.save(question);
    }

    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

        question.setContent(questionDetails.getContent());
        question.setOptions(questionDetails.getOptions());
        question.setAnswer(questionDetails.getAnswer());
        question.setType(normalizeType(questionDetails.getType()));
        question.setCategoryId(questionDetails.getCategoryId());
        question.setCourseId(questionDetails.getCourseId());

        return questionRepository.save(question);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "SINGLE";
        }
        String normalized = type.trim().toUpperCase();
        if ("SINGLE_CHOICE".equals(normalized) || "TRUE_FALSE".equals(normalized)) {
            return "SINGLE";
        }
        if ("MULTIPLE_CHOICE".equals(normalized)) {
            return "MULTIPLE";
        }
        if (!"SINGLE".equals(normalized) && !"MULTIPLE".equals(normalized)) {
            return "SINGLE";
        }
        return normalized;
    }
}
