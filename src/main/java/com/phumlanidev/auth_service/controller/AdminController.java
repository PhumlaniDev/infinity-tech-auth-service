package com.phumlanidev.auth_service.controller;

import com.phumlanidev.auth_service.dto.UserSummaryDto;
import com.phumlanidev.auth_service.service.impl.AdminServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('admin')")
public class AdminController {

    private final AdminServiceImpl adminService;

    @GetMapping
    public ResponseEntity<List<UserSummaryDto>> getAllUsers(Authentication auth) {
        log.info("Authorities = {}", auth.getAuthorities()); // check printed roles
        return ResponseEntity.ok(adminService.getAllUsers());
    }
}
