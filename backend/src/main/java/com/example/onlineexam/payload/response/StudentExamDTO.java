package com.example.onlineexam.payload.response;

import com.example.onlineexam.model.Course;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamDTO {
    private Long id;
    private Course course;
    private String title;
    private int durationInMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<StudentQuestionDTO> questions;
}
