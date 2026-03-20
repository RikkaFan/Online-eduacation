package com.example.onlineexam.aspect;

import com.example.onlineexam.annotation.LogAction;
import com.example.onlineexam.model.OperationLog;
import com.example.onlineexam.repository.OperationLogRepository;
import com.example.onlineexam.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    @Autowired
    private OperationLogRepository operationLogRepository;

    @AfterReturning(value = "@annotation(logAction)", argNames = "joinPoint,logAction")
    public void afterReturning(JoinPoint joinPoint, LogAction logAction) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (logAction == null) {
            try {
                Method targetMethod = joinPoint.getTarget()
                        .getClass()
                        .getMethod(method.getName(), method.getParameterTypes());
                logAction = targetMethod.getAnnotation(LogAction.class);
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (logAction == null) {
            return;
        }

        try {
            OperationLog log = new OperationLog();
            log.setUsername(resolveUsername());
            log.setOperation(logAction.value());
            log.setMethod(resolveHttpMethod());
            log.setIp(resolveIp());
            log.setCreateTime(LocalDateTime.now());
            operationLogRepository.save(log);
        } catch (Exception e) {
            logger.error("写入操作日志失败: {}", e.getMessage(), e);
        }
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return "anonymous";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return userDetails.getUsername();
        }
        return String.valueOf(principal);
    }

    private String resolveHttpMethod() {
        HttpServletRequest request = resolveRequest();
        return request == null ? "N/A" : request.getMethod();
    }

    private String resolveIp() {
        HttpServletRequest request = resolveRequest();
        if (request == null) {
            return "N/A";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private HttpServletRequest resolveRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
