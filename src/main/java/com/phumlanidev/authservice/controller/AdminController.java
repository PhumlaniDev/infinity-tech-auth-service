package com.phumlanidev.authservice.controller;

import com.phumlanidev.authservice.dto.AuditLogDto;
import com.phumlanidev.authservice.dto.UserSummaryDto;
import com.phumlanidev.authservice.service.impl.AdminServiceImpl;
import com.phumlanidev.authservice.service.impl.AuditLogServiceImpl;
import java.util.List;
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
