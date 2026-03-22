package com.example.onlineexam.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "chapters")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer sortOrder;

    private String materialName;

    private String materialContentType;

    private Long materialSize;

    @Lob
    @JsonIgnore
    @Column(columnDefinition = "LONGBLOB")
    private byte[] materialData;
}
