package com.phumlanidev.authservice.service.impl;

import com.phumlanidev.authservice.dto.UserProfileDto;
import com.phumlanidev.authservice.helper.KeycloakAdminHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl {

    private final KeycloakAdminHelper keycloakAdminHelper;
    private final AuditLogServiceImpl auditLogService;
    private final HttpServletRequest request;

    public UserProfileDto getCurrentUserProfile(JwtAuthenticationToken token) {
        String userId = token.getToken().getSubject();
        UserRepresentation user = keycloakAdminHelper.getUserById(userId);

        return new UserProfileDto(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }

    public void updateUserProfile(UserProfileDto userProfileDto, JwtAuthenticationToken token) {
        String userId = keycloakAdminHelper.getUserIdFromPrincipal(token);
        UserRepresentation user = keycloakAdminHelper.getUserById(userId);
        String ipAddress = request.getRemoteAddr();

        user.setFirstName(userProfileDto.getFirstName());
        user.setLastName(userProfileDto.getLastName());
        user.setEmail(userProfileDto.getEmail());

        keycloakAdminHelper.updateUser(userId, user);

        auditLogService.log("PROFILE_UPDATED", userId, user.getUsername(), ipAddress, "Updated profile info");
    }

    public void sendPasswordChangeEmail(JwtAuthenticationToken token) {
        String userId = keycloakAdminHelper.getUserIdFromPrincipal(token);
        UserRepresentation user = keycloakAdminHelper.getUserById(userId);
        String ipAddress = request.getRemoteAddr();

        keycloakAdminHelper.triggerPasswordReset(userId);
        auditLogService.log("PASSWORD_RESET_REQUESTED", userId, user.getUsername(), ipAddress, "Sent password reset link");
    }
}
