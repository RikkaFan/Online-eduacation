package com.example.onlineexam.service;

import com.example.onlineexam.model.Question;
import com.example.onlineexam.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));

        question.setContent(questionDetails.getContent());
        question.setOptions(questionDetails.getOptions());
        question.setAnswer(questionDetails.getAnswer());
        question.setCategoryId(questionDetails.getCategoryId());

        return questionRepository.save(question);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }
}
