package com.phumlanidev.auth_service.service;

import com.phumlanidev.auth_service.dto.UpdateProfileRequest;
import com.phumlanidev.auth_service.dto.UserProfileDto;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;

/**
 * Comment: this is the placeholder for documentation.
 */
public interface IProfileService {

    UserProfileDto getCurrentUserProfile(JwtAuthenticationToken token);

    void updateUserProfile(UpdateProfileRequest request, Principal principal);

    void sendPasswordChangeEmail(Principal principal);
}
