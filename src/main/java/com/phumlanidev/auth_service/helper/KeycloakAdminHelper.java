package com.phumlanidev.auth_service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminHelper {

    private final Keycloak keycloak;
    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;
    @Value("${keycloak.realm}")
    private String keycloakRealm;
    @Value("${keycloak.admin.username}")
    private String keycloakAdminUsername;
    @Value("${keycloak.admin.password}")
    private String keycloakAdminPassword;

    public String getUserIdByUsername(String username) {
        try (Keycloak adminClient = getAdminClient()){
            List<UserRepresentation> users = getUserResource(adminClient).search(username, true);
            return users.stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst()
                    .map(UserRepresentation::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error getting user ID for username {}: {}", username, e.getMessage());
            return null;
        }
    }

    public String getUserIdFromPrincipal(JwtAuthenticationToken token) {
        return token.getToken().getSubject();
    }

    public UserRepresentation getUserById(String userId) {
        try (Keycloak adminClient = getAdminClient()) {
            return getRealm(adminClient).users().get(userId).toRepresentation();
        } catch (Exception e) {
            log.error("Error getting user by ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private Keycloak getAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakServerUrl)
                .realm("master")
                .username(keycloakAdminUsername)
                .password(keycloakAdminPassword)
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .build();
    }

    private UsersResource getUserResource(Keycloak keycloak) {
        return getRealm(keycloak).users();
    }

    private RealmResource getRealm(Keycloak adminClient) {
        return adminClient.realm(keycloakRealm);
    }

    public void updateUser(String userId, UserRepresentation userRepresentation) {
        keycloak.realm(keycloakRealm).users().get(userId).update(userRepresentation);
    }

    public void triggerPasswordReset(String userId) {
        keycloak.realm(keycloakRealm).users().get(userId).executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));

    }
}
