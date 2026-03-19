package com.example.onlineexam.controller;

import com.example.onlineexam.model.OperationLog;
import com.example.onlineexam.repository.OperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OperationLog> getLogs() {
        return operationLogRepository.findAllByOrderByCreateTimeDesc();
    }
}
