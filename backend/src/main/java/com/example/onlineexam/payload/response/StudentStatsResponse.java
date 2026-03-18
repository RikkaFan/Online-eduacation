package com.example.onlineexam.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatsResponse {
    private long attendedExams;
    private long totalMistakes;
    private Double averageScore;
}
