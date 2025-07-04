package com.phumlanidev.authservice.service.impl;


import com.phumlanidev.authservice.config.JwtAuthenticationConverter;
import com.phumlanidev.authservice.dto.*;
import com.phumlanidev.authservice.enums.RoleMapping;
import com.phumlanidev.authservice.exception.auth.AuthenticationFailedException;
import com.phumlanidev.authservice.exception.auth.KeycloakCommunicationException;
import com.phumlanidev.authservice.helper.KeycloakAdminHelper;
import com.phumlanidev.authservice.mapper.AddressMapper;
import com.phumlanidev.authservice.mapper.UserMapper;
import com.phumlanidev.authservice.model.Address;
import com.phumlanidev.authservice.model.User;
import com.phumlanidev.authservice.repository.AddressRepository;
import com.phumlanidev.authservice.repository.UserRepository;
import com.phumlanidev.authservice.service.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;


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
  private final HttpServletRequest request;
  private final AuditLogServiceImpl auditLogService;
  private final KeycloakAdminHelper keycloakAdminHelper;
  private final RestTemplate restTemplate;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

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

  @Override
  public void registerUser(UserDto userDto) {
    String rawPassword = userDto.getPassword();
    userDto.setPassword(passwordEncoder.encode(rawPassword));

    User user = userMapper.toEntity(userDto, new User());
    Address address = addressMapper.toEntity(userDto.getAddress(), new Address());

    Address savedAddress = addressRepository.save(address);
    user.setAddress(savedAddress);
    userRepository.save(user);

    logAudit("USER_REGISTRATION",
            "User registered successfully: " + userDto.getUsername());

    registerKeycloakUser(userDto, rawPassword);

    sendEmailVerificationNotification(userDto.getEmail());

  }

  @Override
  public JwtResponseDto login(LoginDto loginDto) {
    String userId = keycloakAdminHelper.getUserIdByUsername(loginDto.getUsername());

    try (Keycloak keycloakClient = KeycloakBuilder.builder().serverUrl(keycloakServerUrl)
            .realm(keycloakRealm).clientId(keycloakClientId).clientSecret(keycloakClientSecret)
            .grantType(OAuth2Constants.PASSWORD).username(loginDto.getUsername())
            .password(loginDto.getPassword()).build()) {

      AccessTokenResponse tokenResponse = keycloakClient.tokenManager().grantToken();

      logAudit("LOGIN_SUCCESS",
              "User: " + loginDto.getUsername() + " logged in successfully, UserId: " + userId);

      return new JwtResponseDto(
              tokenResponse.getToken(),
              tokenResponse.getRefreshToken(),
              tokenResponse.getExpiresIn()
      );
    } catch (Exception e) {
      log.error("Exception occurred while logging in user {}: {}", loginDto.getUsername(),
              e.getMessage(), e);
      logAudit("LOGIN_FAIL",
              "Login failed for user: " + loginDto.getUsername() + ", Error: " + e.getMessage());
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
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth != null ? auth.getName() : "anonymous";

    try {
      ResponseEntity<String> response = restTemplate.postForEntity(logoutUri, entity, String.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("Logout successful for refresh token: {}", refreshToken.getRefreshToken());
        logAudit("LOGOUT_SUCCESS",
                "User: " + username + " logged out successfully");
      } else {
        log.error("Logout failed with response: {}", response.getBody());
        logAudit("LOGOUT_FAIL",
                "Logout failed for user: " + username);
      }
    } catch (Exception e) {
      log.error("Exception occurred during logout: {}", e.getMessage(), e);
      throw new RuntimeException("Logout failed due to an exception", e);
    }
  }

  @Override
  public void sendPasswordResetNotification(String email) {
    String url = "http://localhost:9500/api/v1/notifications/password-reset";
    PasswordResetRequestDto passwordResetDto = PasswordResetRequestDto.builder().email(email).build();

    try {
      String token = jwtAuthenticationConverter.getCurrentJwt().getTokenValue();

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(token);
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<PasswordResetRequestDto> requestDtoHttpEntity = new HttpEntity<>(passwordResetDto, headers);

      restTemplate.postForEntity(url, requestDtoHttpEntity, Void.class);
      log.info("Password reset notification sent to {}", email);
    } catch (Exception e) {
      log.error("Failed to send password reset notification to {}: {}", email, e.getMessage());
    }
  }

  @Override
  public void sendEmailVerificationNotification(String email) {
    String url = "http://localhost:9500/api/v1/notifications/email-verification";
    PasswordResetRequestDto emailVerificationDto = PasswordResetRequestDto.builder().email(email).build();

    try {
      String token = jwtAuthenticationConverter.getCurrentJwt().getTokenValue();

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(token);
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<PasswordResetRequestDto> requestDtoHttpEntity = new HttpEntity<>(emailVerificationDto, headers);

      restTemplate.postForEntity(url, requestDtoHttpEntity, Void.class);
      log.info("Email verification notification sent to {}", email);
    } catch (Exception e) {
      log.error("Failed to send email verification notification to {}: {}", email, e.getMessage());
    }
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
        logAudit("USER_CREATION_SUCCESS",
                "User created successfully in Keycloak: " + userDto.getUsername());
      }
    } catch (Exception e) {
      log.error("Exception occurred while creating user {} in Keycloak: {}", userDto.getUsername(),
          e.getMessage(), e);
      logAudit("USER_CREATION_FAIL",
              "Failed to create user in Keycloak: " + userDto.getUsername() + ", Error: " + e.getMessage());
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

    String clientUuid = clients.getFirst().getId();

    ClientResource clientResource = realmResource.clients().get(clientUuid);

    RoleRepresentation clientRole = clientResource.roles().get(clientRoleName).toRepresentation();

    userResource.roles().clientLevel(clientUuid).add(Collections.singletonList(clientRole));
  }

  private void logAudit(String action, String details) {
    String clientIp = request.getRemoteAddr();
    String username = jwtAuthenticationConverter.getCurrentUsername();
    String userId = jwtAuthenticationConverter.getCurrentUserId();


    auditLogService.log(
            action,
            userId,
            username,
            clientIp,
            details
    );
  }
}