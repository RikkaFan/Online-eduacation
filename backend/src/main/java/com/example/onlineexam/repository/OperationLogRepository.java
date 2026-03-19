package com.example.onlineexam.repository;

import com.example.onlineexam.model.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findAllByOrderByCreateTimeDesc();
}
