package com.phumlanidev.auth_service.controller;

import com.phumlanidev.auth_service.dto.UserProfileDto;
import com.phumlanidev.auth_service.service.impl.ProfileServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comment: this is the placeholder for documentation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('user')")
public class ProfileController {

    /**
     * Comment: this is the placeholder for documentation.
     */
    private final ProfileServiceImpl profileService;

    /**
     * Comment: this is the placeholder for documentation.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUserProfile(JwtAuthenticationToken token) {
        log.info("✅ Roles: {}", token.getAuthorities());
        UserProfileDto profile = profileService.getCurrentUserProfile(token);
        return ResponseEntity.ok(profile);
    }
}
