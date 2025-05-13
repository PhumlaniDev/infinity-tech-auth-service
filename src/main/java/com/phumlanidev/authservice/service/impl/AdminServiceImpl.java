package com.phumlanidev.authservice.service.impl;

import com.phumlanidev.authservice.dto.UserDto;
import com.phumlanidev.authservice.dto.UserSummaryDto;
import com.phumlanidev.authservice.service.IAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements IAdminService {

    private final Keycloak keycloak;
    private final AuthServiceImpl authService;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    public List<UserSummaryDto> getAllUsers() {
        return keycloak.realm(realm).users().list().stream()
                .map(user -> new UserSummaryDto(user.getId(), user.getUsername(), user.getEmail(), user.isEnabled()))
                .collect(Collectors.toList());
    }

    @Override
    public void createUser(UserDto userDto) {
        authService.registerUser(userDto);
    }

    @Override
    public void updateUserRoles(String userId, List<String> roles) {
        UserResource user = keycloak.realm(realm).users().get(userId);
        RealmResource realmResource = keycloak.realm(realm);
        List<RoleRepresentation> toAssign = roles.stream()
                .map(role -> realmResource.roles().get(role).toRepresentation())
                .toList();

        user.roles().realmLevel().remove(user.roles().realmLevel().listAll());
        user.roles().realmLevel().add(toAssign);
    }

    @Override
    public void setUserEnabled(String userId, boolean enable) {
        UserResource user = keycloak.realm(realm).users().get(userId);
        UserRepresentation rep = user.toRepresentation();
        rep.setEnabled(enable);
        user.update(rep);

    }

    @Override
    public void deleteUser(String userId) {
        try (var response = keycloak.realm(realm).users().delete(userId)) {
            log.info("User with id {} deleted successfully", userId);
        } catch (Exception e) {
            log.error("Failed to delete user with id {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to delete user");
        }
    }
}
