package com.nqd.nqd_tool_content.service;

import com.nqd.nqd_tool_content.entity.AuditLog;
import com.nqd.nqd_tool_content.repository.AuditLogRepository;
import com.nqd.nqd_tool_content.util.Redactor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, String action, String entityType, String entityId, String details) {
        logAction(userId, action, entityType, entityId, details, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(UUID userId, String action, String entityType, String entityId, String details, HttpServletRequest request) {
        String ip = null;
        String userAgent = null;
        if (request != null) {
            ip = request.getRemoteAddr();
            userAgent = request.getHeader("User-Agent");
        }

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(Redactor.redact(details))
                .ip(ip)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit: action={}, userId={}, entityType={}, entityId={}", action, userId, entityType, entityId);
    }
}
