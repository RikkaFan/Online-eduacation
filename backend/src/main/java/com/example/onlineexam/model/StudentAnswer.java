package com.example.onlineexam.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "student_answers")
public class StudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Lob
    private String selectedAnswer;

    private Integer score;

    private Boolean graded;
}
