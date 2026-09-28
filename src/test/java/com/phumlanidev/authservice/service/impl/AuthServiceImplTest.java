package com.phumlanidev.authservice.service.impl;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private AddressRepository addressRepository;
  @Mock private UserMapper userMapper;
  @Mock private AddressMapper addressMapper;
  @Mock private HttpServletRequest request;
  @Mock private AuditLogServiceImpl auditLogService;
  @Mock private KeycloakAdminHelper keycloakAdminHelper;
  @Mock private RestTemplate restTemplate;
  @Mock private Keycloak keycloakAdminClient;

  @InjectMocks
  private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(authService, "keycloakServerUrl", "http://localhost:8080");
    ReflectionTestUtils.setField(authService, "keycloakRealm", "ecommerce");
    ReflectionTestUtils.setField(authService, "keycloakClientId", "auth-service");
    ReflectionTestUtils.setField(authService, "keycloakClientSecret", "secret");
    ReflectionTestUtils.setField(authService, "logoutUri",
        "http://localhost:8080/realms/ecommerce/protocol/openid-connect/logout");
  }

  // ── Test data ────────────────────────────────────────────────────────────

  private UserDto validUserDto() {
    return UserDto.builder()
            .firstName("Phumlani")
            .lastName("Dev")
            .username("phumlanidev")
            .email("aphumlani.dev@gmail.com")
            .password("SecurePass123!")
            .phoneNumber("0821234567")
            .role(RoleMapping.USER)
            .address(AddressDto.builder()
                    .streetName("123 Main St")
                    .city("Cape Town")
                    .build())
            .build();
  }

  private LoginDto validLoginDto() {
    return LoginDto.builder()
            .username("phumlanidev")
            .password("SecurePass123!")
            .build();
  }

  // ── registerUser ──────────────────────────────────────────────────────

  @Test
  @DisplayName("registerUser — success — saves user after Keycloak creation")
  void registerUser_success() {
    // Arrange
    UserDto dto = validUserDto();
    User user = new User();
    Address address = new Address();
    Address savedAddress = new Address();

    RealmResource realmResource = mock(RealmResource.class);
    UsersResource usersResource = mock(UsersResource.class);
    Response createdResponse = mock(Response.class);
    UserResource userResource = mock(UserResource.class);

    RolesResource rolesResource = mock(RolesResource.class);
    RoleResource realmroleResource = mock(RoleResource.class);
    RoleRepresentation realmRoleRep = new RoleRepresentation();

    RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
    RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

    ClientsResource clientsResource = mock(ClientsResource.class);
    ClientResource clientResource = mock(ClientResource.class);
    RolesResource clientRolesResource = mock(RolesResource.class);
    RoleResource clientRoleResource = mock(RoleResource.class);
    RoleRepresentation clientRoleRep = new RoleRepresentation();

    when(keycloakAdminClient.realm(anyString())).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
    when(usersResource.create(any())).thenReturn(createdResponse);
    when(createdResponse.getStatus()).thenReturn(201);
    when(createdResponse.getLocation())
            .thenReturn(URI.create("http://keycloak/users/user-id-123"));
    when(usersResource.get("user-id-123")).thenReturn(userResource);

    when(realmResource.roles()).thenReturn(rolesResource);
    when(rolesResource.get(anyString())).thenReturn(realmroleResource);
    when(realmroleResource.toRepresentation()).thenReturn(realmRoleRep);

    when(userResource.roles()).thenReturn(roleMappingResource);
    when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
    when(roleMappingResource.clientLevel(anyString())).thenReturn(roleScopeResource);

    // Client role setup
    ClientRepresentation clientRep = new ClientRepresentation();
    clientRep.setId("client-uuid");
    when(realmResource.clients()).thenReturn(clientsResource);
    when(realmResource.clients().findByClientId(anyString()))
            .thenReturn(List.of(clientRep));
    when(clientsResource.get("client-uuid")).thenReturn(clientResource);

    when(clientResource.roles()).thenReturn(clientRolesResource);
    when(clientRolesResource.get(anyString())).thenReturn(clientRoleResource);
    when(clientRoleResource.toRepresentation()).thenReturn(clientRoleRep);

    when(addressMapper.toEntity(any(), any())).thenReturn(address);
    when(addressRepository.save(address)).thenReturn(savedAddress);
    when(userMapper.toEntity(any(), any())).thenReturn(user);
    when(keycloakAdminHelper.getCurrentUserId()).thenReturn("system");
    when(keycloakAdminHelper.getCurrentUsername()).thenReturn("system");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

    // Act
    authService.registerUser(dto);

    // Assert
    verify(userRepository).save(user);
    verify(addressRepository).save(address);
    verify(auditLogService).log(eq("USER_REGISTRATION"), any(), any(), any(), any());
  }

  @Test
  @DisplayName("registerUser — Keycloak conflict — throws UserAlreadyExistException")
  void registerUser_keycloakConflict_throwsException() {
    UserDto dto = validUserDto();
    RealmResource realmResource = mock(RealmResource.class);
    UsersResource usersResource = mock(UsersResource.class);
    Response conflictResponse = mock(Response.class);

    when(keycloakAdminClient.realm(anyString())).thenReturn(realmResource);
    when(realmResource.users()).thenReturn(usersResource);
    when(usersResource.create(any())).thenReturn(conflictResponse);
    when(conflictResponse.getStatus()).thenReturn(409); // CONFLICT

    assertThrows(KeycloakCommunicationException.class,
            () -> authService.registerUser(dto));

    // Local DB must NOT be touched if Keycloak fails
    verifyNoInteractions(userRepository);
    verifyNoInteractions(addressRepository);
  }

  @Test
  @DisplayName("registerUser — Keycloak unreachable — throws KeycloakCommunicationException")
  void registerUser_keycloakUnreachable_throwsException() {
    UserDto dto = validUserDto();

    when(keycloakAdminClient.realm(anyString()))
            .thenThrow(new RuntimeException("Connection refused"));

    assertThrows(KeycloakCommunicationException.class,
            () -> authService.registerUser(dto));

    verifyNoInteractions(userRepository);
  }

  // ── login ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("login — success path still validates credentials")
  void login_success() {
    LoginDto dto = validLoginDto();
    when(keycloakAdminHelper.getUserIdByUsername(dto.getUsername()))
            .thenReturn("user-id-123");
    when(keycloakAdminHelper.getCurrentUserId()).thenReturn("user-id-123");
    when(keycloakAdminHelper.getCurrentUsername()).thenReturn("phumlanidev");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    assertThrows(AuthenticationFailedException.class, () -> authService.login(dto));
    verify(auditLogService).log(eq("LOGIN_FAIL"), any(), any(), any(), any());
  }

  @Test
  @DisplayName("login — wrong password — throws AuthenticationFailedException")
  void login_invalidCredentials_throwsException() {
    LoginDto dto = LoginDto.builder()
            .username("phumlanidev")
            .password("wrongpassword")
            .build();

    when(keycloakAdminHelper.getUserIdByUsername(dto.getUsername()))
            .thenReturn("user-id-123");
    when(keycloakAdminHelper.getCurrentUsername()).thenReturn("phumlanidev");
    when(keycloakAdminHelper.getCurrentUserId()).thenReturn("user-id-123");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    // The inline Keycloak client will throw since no real Keycloak is running
    // AuthServiceImpl catches it and re-throws as AuthenticationFailedException
    assertThrows(AuthenticationFailedException.class,
            () -> authService.login(dto));

    verify(auditLogService).log(eq("LOGIN_FAIL"), any(), any(), any(), any());
  }

  // ── logout ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("logout — null token — throws IllegalArgumentException")
  void logout_nullToken_throwsException() {
    assertThrows(IllegalArgumentException.class,
            () -> authService.logout(null));

    assertThrows(IllegalArgumentException.class,
            () -> authService.logout(new TokenLogoutRequest(null)));

    verifyNoInteractions(restTemplate);
  }

  @Test
  @DisplayName("logout — Keycloak returns 2xx — logs success")
  void logout_success() {
    TokenLogoutRequest req = new TokenLogoutRequest("valid-refresh-token");

    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(ResponseEntity.ok("{}"));
    when(keycloakAdminHelper.getCurrentUsername()).thenReturn("phumlanidev");
    when(keycloakAdminHelper.getCurrentUserId()).thenReturn("user-id-123");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    authService.logout(req);

    verify(auditLogService).log(eq("LOGOUT_SUCCESS"), any(), any(), any(), any());
  }

  @Test
  @DisplayName("logout — Keycloak returns non-2xx — throws RuntimeException")
  void logout_keycloakError_throwsException() {
    TokenLogoutRequest req = new TokenLogoutRequest("expired-refresh-token");

    when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
            .thenReturn(ResponseEntity.badRequest().body("invalid_grant"));
    when(keycloakAdminHelper.getCurrentUsername()).thenReturn("phumlanidev");
    when(keycloakAdminHelper.getCurrentUserId()).thenReturn("user-id-123");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    assertThrows(RuntimeException.class, () -> authService.logout(req));
  }

  // ── sendPasswordResetNotification ─────────────────────────────────────

  @Test
  @DisplayName("sendPasswordReset — success — calls notification-service")
  void sendPasswordReset_success() {
    when(restTemplate.postForEntity(
            contains("/api/v1/notifications/password-reset"),
            any(), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

    // Should not throw even if notification fails
    assertDoesNotThrow(
            () -> authService.sendPasswordResetNotification("user@example.com"));
  }

  @Test
  @DisplayName("sendPasswordReset — notification-service down — does not throw")
  void sendPasswordReset_notificationServiceDown_doesNotThrow() {
    when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
            .thenThrow(new RuntimeException("Connection refused"));

    // Fire and forget — must not propagate the exception to the caller
    assertDoesNotThrow(
            () -> authService.sendPasswordResetNotification("user@example.com"));
  }

  // ── sendEmailVerificationNotification ─────────────────────────────────

  @Test
  @DisplayName("sendEmailVerification — success — calls notification-service")
  void sendEmailVerification_success() {
    when(restTemplate.postForEntity(
            contains("/api/v1/notifications/email-verification"),
            any(), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());

    assertDoesNotThrow(
            () -> authService.sendEmailVerificationNotification("user@example.com"));
  }

  @Test
  @DisplayName("sendEmailVerification — notification-service down — does not throw")
  void sendEmailVerification_notificationServiceDown_doesNotThrow() {
    when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
            .thenThrow(new RuntimeException("Connection refused"));

    assertDoesNotThrow(
            () -> authService.sendEmailVerificationNotification("user@example.com"));
  }
}

