package com.example.onlineexam.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    private int durationInMinutes;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer singleCount;
    private Integer singleScore;
    private Integer multipleCount;
    private Integer multipleScore;
    private Integer judgeCount;
    private Integer judgeScore;
    private Integer subjectiveCount;
    private Integer subjectiveScore;
    private Integer totalScore;

    @ManyToMany
    @JoinTable(
        name = "exam_questions",
        joinColumns = @JoinColumn(name = "exam_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<Question> questions;
}
