package com.phumlanidev.auth_service.controller;

import com.phumlanidev.auth_service.dto.AuditLogDto;
import com.phumlanidev.auth_service.dto.UserSummaryDto;
import com.phumlanidev.auth_service.service.impl.AdminServiceImpl;
import com.phumlanidev.auth_service.service.impl.AuditLogServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('admin')")
public class AdminController {

    private final AdminServiceImpl adminService;
    private final AuditLogServiceImpl auditLogService;

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDto>> getAllUsers(Authentication auth) {
        log.info("Authorities = {}", auth.getAuthorities()); // check printed roles
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> getLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(auditLogService.getAuditLogs(userId, action, pageable));
    }
}
