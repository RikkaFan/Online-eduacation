package com.example.onlineexam.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuestionDTO {
    private Long id;
    private Long categoryId;
    private String content;
    private String type;
    private String options;
    private String answer;
    private String analysis;
}
