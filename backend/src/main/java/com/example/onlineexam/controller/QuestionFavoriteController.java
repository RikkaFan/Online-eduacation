package com.example.onlineexam.controller;

import com.example.onlineexam.model.Question;
import com.example.onlineexam.model.QuestionFavorite;
import com.example.onlineexam.repository.QuestionFavoriteRepository;
import com.example.onlineexam.repository.QuestionRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/favorites")
public class QuestionFavoriteController {

    @Autowired
    private QuestionFavoriteRepository questionFavoriteRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @PostMapping("/{questionId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public Map<String, Object> toggleFavorite(@PathVariable Long questionId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        int removed = questionFavoriteRepository.deleteByUserIdAndQuestionId(userId, questionId);
        if (removed > 0) {
            return Map.<String, Object>of("favorited", false, "questionId", questionId);
        }

        Question question = questionRepository.findByIdAndDeletedFalse(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "题目不存在或已删除"));
        QuestionFavorite favorite = new QuestionFavorite();
        favorite.setUserId(userId);
        favorite.setQuestion(question);
        favorite.setCreateTime(LocalDateTime.now());
        try {
            questionFavoriteRepository.save(favorite);
        } catch (DataIntegrityViolationException e) {
            return Map.<String, Object>of("favorited", true, "questionId", questionId);
        }
        return Map.<String, Object>of("favorited", true, "questionId", questionId);
    }

    @GetMapping("/check/{questionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public boolean checkFavorite(@PathVariable Long questionId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        return questionFavoriteRepository.existsByUserIdAndQuestionId(userId, questionId);
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<QuestionFavorite> getFavorites() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        return questionFavoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }
}
