package com.phumlanidev.auth_service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakClientService {

    private final Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realm;

    public String getUserIdFromPrincipal(JwtAuthenticationToken token) {
        return token.getToken().getSubject();
    }

    public UserRepresentation getUserById(String userId) {
        return keycloak.realm(realm).users().get(userId).toRepresentation();
    }

    public void updateUser(String userId, UserRepresentation userRepresentation) {
        keycloak.realm(realm).users().get(userId).update(userRepresentation);
    }

    public void triggerPasswordReset(String userId) {
        keycloak.realm(realm).users().get(userId).executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
    }
}
