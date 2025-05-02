package com.phumlanidev.auth_service.service.impl;

import com.phumlanidev.auth_service.model.AuditLog;
import com.phumlanidev.auth_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl {

    private final AuditLogRepository auditLogRepository;

    public void log(String action, String userId, String username, String ipAddress, String details) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .details(details)
                .timestamp(Instant.now())
                .build();

        auditLogRepository.save(log);
    }
}
