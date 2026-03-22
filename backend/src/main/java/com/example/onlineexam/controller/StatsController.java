package com.example.onlineexam.controller;

import com.example.onlineexam.payload.response.AdminStatsResponse;
import com.example.onlineexam.payload.response.StudentStatsResponse;
import com.example.onlineexam.security.UserDetailsImpl;
import com.example.onlineexam.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new AccessDeniedException("无权查看统计数据");
        }
        return statsService.getAdminStats(userDetails.getId(), isTeacherOnly());
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentStatsResponse getStudentStats(@PathVariable Long studentId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new AccessDeniedException("无权查看他人的统计数据");
        }
        if (!studentId.equals(userDetails.getId())) {
            throw new AccessDeniedException("无权查看他人的统计数据");
        }
        return statsService.getStudentStats(userDetails.getId());
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
}
