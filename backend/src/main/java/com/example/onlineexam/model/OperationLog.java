package com.example.onlineexam.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(nullable = false)
    private String operation;

    private String method;

    private String ip;

    @Column(nullable = false)
    private LocalDateTime createTime;
}
