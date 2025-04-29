package com.phumlanidev.auth_service.service.impl;

import com.phumlanidev.auth_service.dto.UserProfileDto;
import com.phumlanidev.auth_service.helper.KeycloakClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl {

    private final KeycloakClientService keycloakClient;

    public UserProfileDto getCurrentUserProfile(JwtAuthenticationToken token) {
        String userId = token.getToken().getSubject();
        UserRepresentation user = keycloakClient.getUserById(userId);

        return new UserProfileDto(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }

    public void updateUserProfile(UserProfileDto request, JwtAuthenticationToken token) {
        String userId = keycloakClient.getUserIdFromPrincipal(token);
        UserRepresentation user = keycloakClient.getUserById(userId);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());

        keycloakClient.updateUser(userId, user);
    }

    public void sendPasswordChangeEmail(JwtAuthenticationToken token) {
        String userId = keycloakClient.getUserIdFromPrincipal(token);
        keycloakClient.triggerPasswordReset(userId);
    }
}
