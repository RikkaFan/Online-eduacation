package com.example.onlineexam.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalCourses;
    private long totalExams;
    private long totalQuestions;
    private long totalUsers;
}
