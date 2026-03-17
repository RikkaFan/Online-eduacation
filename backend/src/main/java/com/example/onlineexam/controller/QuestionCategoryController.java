package com.example.onlineexam.controller;

import com.example.onlineexam.model.QuestionCategory;
import com.example.onlineexam.service.QuestionCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/question-categories")
public class QuestionCategoryController {

    @Autowired
    private QuestionCategoryService questionCategoryService;

    @GetMapping
    public List<QuestionCategory> getAllCategories() {
        return questionCategoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionCategory> getCategoryById(@PathVariable Long id) {
        QuestionCategory category = questionCategoryService.getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("QuestionCategory not found with id: " + id));
        return ResponseEntity.ok(category);
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public QuestionCategory createCategory(@RequestBody QuestionCategory category) {
        return questionCategoryService.createCategory(category);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<QuestionCategory> updateCategory(@PathVariable Long id, @RequestBody QuestionCategory categoryDetails) {
        QuestionCategory updatedCategory = questionCategoryService.updateCategory(id, categoryDetails);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        questionCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
