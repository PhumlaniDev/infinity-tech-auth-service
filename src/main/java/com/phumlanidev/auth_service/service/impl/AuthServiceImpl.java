package com.phumlanidev.auth_service.service.impl;


import com.phumlanidev.auth_service.dto.JwtResponseDto;
import com.phumlanidev.auth_service.dto.LoginDto;
import com.phumlanidev.auth_service.dto.TokenLogoutRequest;
import com.phumlanidev.auth_service.dto.UserDto;
import com.phumlanidev.auth_service.enums.RoleMapping;
import com.phumlanidev.auth_service.exception.UserNotFoundException;
import com.phumlanidev.auth_service.exception.auth.AuthenticationFailedException;
import com.phumlanidev.auth_service.exception.auth.KeycloakCommunicationException;
import com.phumlanidev.auth_service.exception.auth.UserNotVerified;
import com.phumlanidev.auth_service.mapper.AddressMapper;
import com.phumlanidev.auth_service.mapper.UserMapper;
import com.phumlanidev.auth_service.model.Address;
import com.phumlanidev.auth_service.model.User;
import com.phumlanidev.auth_service.repository.AddressRepository;
import com.phumlanidev.auth_service.repository.UserRepository;
import com.phumlanidev.auth_service.service.IAuthService;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Comment: this is the placeholder for documentation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

  private static final String ENABLED_ATTRIBUTE = "enabled";
  private static final String TRUE_VALUE = "true";
  private final UserRepository userRepository;
  private final AddressRepository addressRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final AddressMapper addressMapper;

  @Value("${keycloak.auth-server-url}")
  private String keycloakServerUrl;
  @Value("${keycloak.realm}")
  private String keycloakRealm;
  @Value("${keycloak.resource}")
  private String keycloakClientId;
  @Value("${keycloak.credentials.secret}")
  private String keycloakClientSecret;
  @Value("${keycloak.admin.username}")
  private String keycloakAdminUsername;
  @Value("${keycloak.admin.password}")
  private String keycloakAdminPassword;
  @Value("${keycloak.logout_uri}")
  private String logoutUri;

  private final RestTemplate restTemplate;


  /**
   * Comment: this is the placeholder for documentation.
   */
  @Override
  public void registerUser(UserDto userDto) {
    String rawPassword = userDto.getPassword();
    userDto.setPassword(passwordEncoder.encode(rawPassword));

    User user = userMapper.toEntity(userDto, new User());
    Address address = addressMapper.toEntity(userDto.getAddress(), new Address());

    Address savedAddress = addressRepository.save(address);
    user.setAddress(savedAddress);
    userRepository.save(user);

    registerKeycloakUser(userDto, rawPassword);

    sendEmailVerification(userDto.getEmail());

  }

  private void registerKeycloakUser(UserDto userDto, String rawPassword) {
    try {
      try (Keycloak adminClient = KeycloakBuilder.builder()
          .serverUrl(keycloakServerUrl)
          .realm("master")
          .clientId("admin-cli")
              .username(keycloakAdminUsername)
              .password(keycloakAdminPassword)
          .grantType(OAuth2Constants.PASSWORD)
          .build()) {
        RealmResource realmResource = adminClient.realm(keycloakRealm);
        UsersResource usersResource = realmResource.users();
        UserRepresentation keycloakUser = createUserRepresentation(userDto, rawPassword);

        createAndAssignKeycloakUser(usersResource, realmResource, keycloakUser, userDto);
      }
    } catch (Exception e) {
      log.error("Exception occurred while creating user {} in Keycloak: {}", userDto.getUsername(),
          e.getMessage(), e);
    }
  }


  private void createAndAssignKeycloakUser(UsersResource usersResource, RealmResource realmResource,
                                           UserRepresentation keycloakUser, UserDto userDto) {
    try (Response response = usersResource.create(keycloakUser)) { // Try-with-resources
      if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
        log.info("Keycloak user created successfully for username: {}", userDto.getUsername());
        String userId = getUserIdFromLocation(response.getLocation());
        UserResource userResource = usersResource.get(userId);

        RoleMapping roleMapping = RoleMapping.from(userDto.getRole().toString())
            .orElseThrow(() -> new KeycloakCommunicationException("Invalid role"));

        assignRealmRole(userResource, realmResource, roleMapping.getRealmRole());
        assignClientRole(userResource, realmResource, roleMapping.getClientRole());
      } else {
        log.error("Failed to create Keycloak user: {}", response.getStatusInfo().toString());
        throw new KeycloakCommunicationException("Keycloak user creation failed");
      }
    } catch (NotAuthorizedException e) {
      log.error("Authorization failed during user creation: {}", e.getMessage());
    }
  }

  private String getUserIdFromLocation(URI location) {
    String path = location.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private UserRepresentation createUserRepresentation(UserDto userDto, String rawPassword) {
    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername(userDto.getUsername());
    userRepresentation.setEmail(userDto.getEmail());
    userRepresentation.setFirstName(userDto.getFirstName());
    userRepresentation.setLastName(userDto.getLastName());
    userRepresentation.singleAttribute(ENABLED_ATTRIBUTE, TRUE_VALUE);
    userRepresentation.setEnabled(true);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setTemporary(false);
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(rawPassword);
    userRepresentation.setCredentials(Collections.singletonList(credential));
    log.info("Password set for user ID {} in Keycloak", userDto.getUsername());
    return userRepresentation;
  }

  private void assignRealmRole(UserResource userResource, RealmResource realmResource,
                               String roleName) {
    RoleRepresentation realmRole = realmResource.roles().get(roleName).toRepresentation();
    userResource.roles().realmLevel().add(Collections.singletonList(realmRole));
  }

  private void assignClientRole(UserResource userResource, RealmResource realmResource,
                                String clientRoleName) {

    List<ClientRepresentation> clients = realmResource.clients().findByClientId(keycloakClientId);
    if (clients.isEmpty()) {
      log.error("Client with ID {} not found in Keycloak", keycloakClientId);
      throw new KeycloakCommunicationException("Client not found");
    }

    String clientUuid = clients.get(0).getId();

    ClientResource clientResource = realmResource.clients().get(clientUuid);

    RoleRepresentation clientRole = clientResource.roles().get(clientRoleName).toRepresentation();

    userResource.roles().clientLevel(clientUuid).add(Collections.singletonList(clientRole));
  }

  /**
   * Comment: this is the placeholder for documentation.
   */
  @Override
  public JwtResponseDto login(LoginDto loginDto) {
    try (Keycloak keycloakClient = KeycloakBuilder.builder().serverUrl(keycloakServerUrl)
        .realm(keycloakRealm).clientId(keycloakClientId).clientSecret(keycloakClientSecret)
        .grantType(OAuth2Constants.PASSWORD).username(loginDto.getUsername())
        .password(loginDto.getPassword()).build()) {

      AccessTokenResponse tokenResponse = keycloakClient.tokenManager().grantToken();

      return new JwtResponseDto(
              tokenResponse.getToken(),
              tokenResponse.getRefreshToken(),
              tokenResponse.getExpiresIn()
      );
    } catch (Exception e) {
      log.error("Exception occurred while logging in user {}: {}", loginDto.getUsername(),
          e.getMessage(), e);
    }
    throw new AuthenticationFailedException("Invalid username or password");
  }

  @Override
  public void logout(TokenLogoutRequest refreshToken) {
    if (refreshToken == null || refreshToken.getRefreshToken() == null) {
      log.error("Refresh token is null. Cannot proceed with logout.");
      throw new IllegalArgumentException("Refresh token must not be null");
    }

    log.info("Attempting to logout user with refresh token: {}", refreshToken.getRefreshToken());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("client_id", keycloakClientId);
    body.add("client_secret", keycloakClientSecret);
    body.add("refresh_token", refreshToken.getRefreshToken());

    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<String> response = restTemplate.postForEntity(logoutUri, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("Logout successful for refresh token: {}", refreshToken.getRefreshToken());
        } else {
            log.error("Logout failed with response: {}", response.getBody());
            throw new RuntimeException("Logout failed");
        }
    } catch (Exception e) {
        log.error("Exception occurred during logout: {}", e.getMessage(), e);
        throw new RuntimeException("Logout failed due to an exception", e);
    }
  }

  @Override
  public void sendResetPasswordEmail(String email) {

    try (Keycloak adminClient = KeycloakBuilder.builder()
            .serverUrl(keycloakServerUrl)
            .realm("master")
            .clientId("admin-cli")
            .username(keycloakAdminUsername)
            .password(keycloakAdminPassword)
            .grantType(OAuth2Constants.PASSWORD)
            .build()) {

      List<UserRepresentation> users = adminClient.realm(keycloakRealm)
              .users()
              .searchByEmail(email, true); // or paginate properly

      UserRepresentation user = users.stream()
              .filter(u -> email.equalsIgnoreCase(u.getEmail()))
              .findFirst()
              .orElseThrow(() -> new UserNotFoundException("User not found"));

      adminClient.realm(keycloakRealm)
              .users()
              .get(user.getId())
                .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
    } catch (Exception e) {
      log.error("Exception occurred while send password reset link to user {}: {}", email,
              e.getMessage(), e);
    }
  }

  private void sendEmailVerification(String email) {
    try (Keycloak adminClient = KeycloakBuilder.builder()
            .serverUrl(keycloakServerUrl)
            .realm("master")
            .clientId("admin-cli")
            .username(keycloakAdminUsername)
            .password(keycloakAdminPassword)
            .grantType(OAuth2Constants.PASSWORD)
            .build()) {

      List<UserRepresentation> users = adminClient.realm(keycloakRealm)
              .users()
              .searchByEmail(email, true); // or paginate properly

      UserRepresentation user = users.stream()
              .filter(u -> email.equalsIgnoreCase(u.getEmail()))
              .findFirst()
              .orElseThrow(() -> new UserNotFoundException("User not found"));

      if (!Boolean.TRUE.equals(user.isEmailVerified())) {
        adminClient
                .realm(keycloakRealm)
                .users()
                .get(user.getId())
                .sendVerifyEmail();
        log.warn("User with email {} is not verified", email);
        throw new UserNotVerified("User not verified");
      }
    } catch (Exception e) {
      log.error("Exception occurred while send password reset link to user {}: {}", email,
              e.getMessage(), e);
    }
  }
}