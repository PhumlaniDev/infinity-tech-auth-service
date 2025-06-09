package com.phumlanidev.authservice.controller;

import com.phumlanidev.authservice.dto.UserProfileDto;
import com.phumlanidev.authservice.service.impl.ProfileServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

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
  public ResponseEntity<UserProfileDto> getCurrentUserProfile(@Valid JwtAuthenticationToken token) {
    log.info("✅ Roles: {}", token.getAuthorities());
    UserProfileDto profile = profileService.getCurrentUserProfile(token);
    return ResponseEntity.ok(profile);
  }

  @PatchMapping("/update")
  public ResponseEntity<Void> updateProfile(@Valid @RequestBody UserProfileDto request, JwtAuthenticationToken token) {
    profileService.updateUserProfile(request, token);
    return ResponseEntity.noContent().build();

  }

  @PostMapping("/change-password")
  public ResponseEntity<Void> sendPasswordChangeEmail(@Valid JwtAuthenticationToken token) {
    profileService.sendPasswordChangeEmail(token);
    return ResponseEntity.noContent().build();
  }
}
