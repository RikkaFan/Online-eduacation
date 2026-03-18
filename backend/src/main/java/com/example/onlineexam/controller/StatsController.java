package com.example.onlineexam.controller;

import com.example.onlineexam.payload.response.AdminStatsResponse;
import com.example.onlineexam.payload.response.StudentStatsResponse;
import com.example.onlineexam.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public AdminStatsResponse getAdminStats() {
        return statsService.getAdminStats();
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentStatsResponse getStudentStats(@PathVariable Long studentId) {
        return statsService.getStudentStats(studentId);
    }
}
