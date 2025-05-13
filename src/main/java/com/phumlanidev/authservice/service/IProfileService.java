package com.phumlanidev.authservice.service;

import com.phumlanidev.authservice.dto.UpdateProfileRequest;
import com.phumlanidev.authservice.dto.UserProfileDto;
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
