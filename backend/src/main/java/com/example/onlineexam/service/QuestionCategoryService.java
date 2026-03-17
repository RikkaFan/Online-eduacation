package com.example.onlineexam.service;

import com.example.onlineexam.model.QuestionCategory;
import com.example.onlineexam.repository.QuestionCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionCategoryService {

    @Autowired
    private QuestionCategoryRepository questionCategoryRepository;

    public List<QuestionCategory> getAllCategories() {
        return questionCategoryRepository.findAll();
    }

    public Optional<QuestionCategory> getCategoryById(Long id) {
        return questionCategoryRepository.findById(id);
    }

    public QuestionCategory createCategory(QuestionCategory category) {
        return questionCategoryRepository.save(category);
    }

    public QuestionCategory updateCategory(Long id, QuestionCategory categoryDetails) {
        QuestionCategory category = questionCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("QuestionCategory not found with id: " + id));
        
        category.setName(categoryDetails.getName());
        
        return questionCategoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        questionCategoryRepository.deleteById(id);
    }
}
