package com.phumlanidev.authservice.service.impl;

import com.phumlanidev.authservice.config.JwtAuthenticationConverter;
import com.phumlanidev.authservice.dto.*;
import com.phumlanidev.authservice.exception.auth.AuthenticationFailedException;
import com.phumlanidev.authservice.helper.KeycloakAdminHelper;
import com.phumlanidev.authservice.mapper.AddressMapper;
import com.phumlanidev.authservice.mapper.UserMapper;
import com.phumlanidev.authservice.model.Address;
import com.phumlanidev.authservice.model.User;
import com.phumlanidev.authservice.repository.AddressRepository;
import com.phumlanidev.authservice.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private AddressRepository addressRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserMapper userMapper;
  @Mock private AddressMapper addressMapper;
  @Mock private HttpServletRequest request;
  @Mock private AuditLogServiceImpl auditLogService;
  @Mock private KeycloakAdminHelper keycloakAdminHelper;
  @Mock private RestTemplate restTemplate;
  @Mock private JwtAuthenticationConverter jwtAuthenticationConverter;

  @InjectMocks
  private AuthServiceImpl authService;

  private static final String USER_ID    = "user-abc";
  private static final String USERNAME   = "phumlani";
  private static final String EMAIL      = "phumlani@example.com";
  private static final String RAW_PASS   = "secret123";
  private static final String ENC_PASS   = "$2a$encoded";
  private static final String CLIENT_IP  = "127.0.0.1";

  @BeforeEach
  void setUp() {
    // Shared audit stubs — lenient so tests that don't verify audit won't fail
    lenient().when(jwtAuthenticationConverter.getCurrentUserId()).thenReturn(USER_ID);
    lenient().when(jwtAuthenticationConverter.getCurrentUsername()).thenReturn(USERNAME);
    lenient().when(request.getRemoteAddr()).thenReturn(CLIENT_IP);
  }

  @Nested
  @DisplayName("registerUser()")
  class RegisterUser {

    private UserDto userDto;
    private User user;
    private Address address;
    private Address savedAddress;

    @BeforeEach
    void setUp() {
      AddressDto addressDto = AddressDto.builder()
              .streetName("123 Main St").city("Cape Town").build();

      userDto = UserDto.builder()
              .username(USERNAME)
              .email(EMAIL)
              .password(RAW_PASS)
              .address(addressDto)
              .build();

      user        = new User();
      address     = new Address();
      savedAddress = new Address();

      when(passwordEncoder.encode(RAW_PASS)).thenReturn(ENC_PASS);
      when(userMapper.toEntity(any(UserDto.class), any(User.class))).thenReturn(user);
      when(addressMapper.toEntity(any(AddressDto.class), any(Address.class))).thenReturn(address);
      when(addressRepository.save(address)).thenReturn(savedAddress);

      // sendEmailVerificationNotification calls getCurrentJwt — stub it
      Jwt jwt = mock(Jwt.class);
      when(jwt.getTokenValue()).thenReturn("mock-token");
      lenient().when(jwtAuthenticationConverter.getCurrentJwt()).thenReturn(jwt);

      // restTemplate for email verification — swallow the call
      lenient().when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenReturn(ResponseEntity.ok().build());
    }

    @Test
    @DisplayName("encodes the raw password before persisting the user")
    void shouldEncodePasswordBeforeSaving() {
      authService.registerUser(userDto);

      verify(passwordEncoder).encode(RAW_PASS);
      // The dto password is replaced with the encoded value before mapping
      assertThat(userDto.getPassword()).isEqualTo(ENC_PASS);
    }

    @Test
    @DisplayName("saves address first, then sets it on the user and saves user")
    void shouldSaveAddressThenUser() {
      authService.registerUser(userDto);

      // Address must be saved before user
      var inOrder = inOrder(addressRepository, userRepository);
      inOrder.verify(addressRepository).save(address);
      inOrder.verify(userRepository).save(user);

      // The saved address is set on the user entity
      assertThat(user.getAddress()).isSameAs(savedAddress);
    }

    @Test
    @DisplayName("maps UserDto to User entity via UserMapper")
    void shouldMapUserDtoToEntity() {
      authService.registerUser(userDto);

      verify(userMapper).toEntity(eq(userDto), any(User.class));
    }

    @Test
    @DisplayName("maps AddressDto to Address entity via AddressMapper")
    void shouldMapAddressDtoToEntity() {
      authService.registerUser(userDto);

      verify(addressMapper).toEntity(eq(userDto.getAddress()), any(Address.class));
    }

    @Test
    @DisplayName("logs USER_REGISTRATION audit event")
    void shouldLogRegistrationAuditEvent() {
      authService.registerUser(userDto);

      verify(auditLogService).log(
              eq("USER_REGISTRATION"),
              eq(USER_ID),
              eq(USERNAME),
              eq(CLIENT_IP),
              contains(USERNAME)
      );
    }

    @Test
    @DisplayName("attempts to send email verification notification after registration")
    void shouldSendEmailVerificationNotification() {
      authService.registerUser(userDto);

      // Verify restTemplate was called with the notification endpoint
      verify(restTemplate).postForEntity(
              contains("email-verification"),
              any(HttpEntity.class),
              eq(Void.class)
      );
    }

    @Test
    @DisplayName("does not throw when email verification notification fails")
    void shouldNotThrowWhenEmailVerificationFails() {
      // Simulate the notification service being unavailable
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenThrow(new RuntimeException("Notification service down"));

      // Registration itself should still succeed — notification failure is swallowed
      assertThatCode(() -> authService.registerUser(userDto))
              .doesNotThrowAnyException();

      // User should still be persisted despite notification failure
      verify(userRepository).save(user);
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // login()
  //
  // NOTE: The Keycloak client is constructed inline via KeycloakBuilder
  // inside login(), making it impossible to mock without refactoring.
  // These tests cover the guard rails around the Keycloak call.
  //
  // RECOMMENDED REFACTOR: Extract Keycloak client creation into a
  // @Bean or a factory method so it can be injected and mocked.
  // ═══════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("login()")
  class Login {

    private LoginDto loginDto;

    @BeforeEach
    void setUp() {
      loginDto = LoginDto.builder()
              .username(USERNAME)
              .password(RAW_PASS)
              .build();
    }

    @Test
    @DisplayName("looks up userId by username before attempting login")
    void shouldLookUpUserIdByUsername() {
      when(keycloakAdminHelper.getUserIdByUsername(USERNAME)).thenReturn(USER_ID);

      // KeycloakBuilder will fail connecting to a real server — the important
      // assertion is that getUserIdByUsername was called first
      try {
        authService.login(loginDto);
      } catch (AuthenticationFailedException ignored) {
        // Expected — no real Keycloak available in unit tests
      }

      verify(keycloakAdminHelper).getUserIdByUsername(USERNAME);
    }

    @Test
    @DisplayName("throws AuthenticationFailedException when Keycloak call fails")
    void shouldThrowAuthenticationFailedExceptionOnKeycloakFailure() {
      when(keycloakAdminHelper.getUserIdByUsername(USERNAME)).thenReturn(USER_ID);

      // KeycloakBuilder will throw because no server is running — this
      // triggers the catch block → AuthenticationFailedException
      assertThatThrownBy(() -> authService.login(loginDto))
              .isInstanceOf(AuthenticationFailedException.class)
              .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("logs LOGIN_FAIL audit event when authentication fails")
    void shouldLogLoginFailAuditEvent() {
      when(keycloakAdminHelper.getUserIdByUsername(USERNAME)).thenReturn(USER_ID);

      try {
        authService.login(loginDto);
      } catch (AuthenticationFailedException ignored) {}

      verify(auditLogService).log(
              eq("LOGIN_FAIL"),
              eq(USER_ID),
              eq(USERNAME),
              eq(CLIENT_IP),
              contains(USERNAME)
      );
    }
  }

  @Nested
  @DisplayName("logout()")
  class Logout {

    private TokenLogoutRequest tokenLogoutRequest;

    @BeforeEach
    void setUpSecurityContext() {
      tokenLogoutRequest = new TokenLogoutRequest("valid-refresh-token");

      ReflectionTestUtils.setField(authService, "logoutUri", "http://lcalhost:8080/realms/test/protocol/openid-connect/logout");
      ReflectionTestUtils.setField(authService, "keycloakClientId", "test-client");
      ReflectionTestUtils.setField(authService, "keycloakClientSecret", "test-secret");

      Authentication auth = mock(Authentication.class);
      lenient().when(auth.getName()).thenReturn(USERNAME);
      SecurityContext securityContext = mock(SecurityContext.class);
      lenient().when(securityContext.getAuthentication()).thenReturn(auth);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when TokenLogoutRequest is null")
    void shouldThrowWhenTokenLogoutRequestIsNull() {
      assertThatThrownBy(() -> authService.logout(null))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Refresh token must not be null");
    }

    @Test
    @DisplayName("throws IllegalArgumentException when refresh token value is null")
    void shouldThrowWhenRefreshTokenValueIsNull() {
      assertThatThrownBy(() -> authService.logout(new TokenLogoutRequest(null)))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Refresh token must not be null");
    }

    @Test
    @DisplayName("sends POST to Keycloak logout URI with correct body")
    void shouldPostToKeycloakLogoutUri() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
              .thenReturn(ResponseEntity.ok(""));

      authService.logout(tokenLogoutRequest);

      ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
      verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));

      // Body must contain the refresh token
      assertThat(Objects.requireNonNull(captor.getValue().getBody()).toString())
              .contains("valid-refresh-token");
    }

    @Test
    @DisplayName("logs LOGOUT_SUCCESS when Keycloak returns 2xx")
    void shouldLogLogoutSuccessOnSuccessfulResponse() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
              .thenReturn(ResponseEntity.ok(""));

      authService.logout(tokenLogoutRequest);

      verify(auditLogService).log(
              eq("LOGOUT_SUCCESS"),
              eq(USER_ID),
              eq(USERNAME),
              eq(CLIENT_IP),
              eq("User: anonymous logged out successfully")
      );
    }

    @Test
    @DisplayName("logs LOGOUT_FAIL when Keycloak returns non-2xx")
    void shouldLogLogoutFailOnNonSuccessResponse() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
              .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("error"));

      authService.logout(tokenLogoutRequest);

      verify(auditLogService).log(
              eq("LOGOUT_FAIL"),
              eq(USER_ID),
              eq(USERNAME),
              eq(CLIENT_IP),
              eq("Logout failed for user: anonymous")
      );
    }

    @Test
    @DisplayName("throws RuntimeException when restTemplate throws during logout")
    void shouldThrowRuntimeExceptionWhenRestTemplateFails() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
              .thenThrow(new RuntimeException("Connection refused"));

      assertThatThrownBy(() -> authService.logout(tokenLogoutRequest))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Logout failed due to an exception");
    }

    @Test
    @DisplayName("uses 'anonymous' when SecurityContext has no authentication")
    void shouldUseAnonymousWhenNoAuthenticationInContext() {
      SecurityContext emptyContext = mock(SecurityContext.class);
      when(emptyContext.getAuthentication()).thenReturn(null);
      SecurityContextHolder.setContext(emptyContext);

      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
              .thenReturn(ResponseEntity.ok(""));

      // Should not throw — falls back to "anonymous" gracefully
      assertThatCode(() -> authService.logout(tokenLogoutRequest))
              .doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("sendPasswordResetNotification()")
  class SendPasswordResetNotification {

    @BeforeEach
    void stubJwt() {
      Jwt jwt = mock(Jwt.class);
      when(jwt.getTokenValue()).thenReturn("mock-token");
      lenient().when(jwtAuthenticationConverter.getCurrentJwt()).thenReturn(jwt);
    }

    @Test
    @DisplayName("sends POST to password-reset notification endpoint")
    void shouldPostToPasswordResetEndpoint() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenReturn(ResponseEntity.ok().build());

      authService.sendPasswordResetNotification(EMAIL);

      ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
      verify(restTemplate).postForEntity(urlCaptor.capture(), any(HttpEntity.class), eq(Void.class));

      assertThat(urlCaptor.getValue()).contains("password-reset");
    }

    @Test
    @DisplayName("sends request with Bearer token Authorization header")
    void shouldSendBearerTokenInHeader() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenReturn(ResponseEntity.ok().build());

      authService.sendPasswordResetNotification(EMAIL);

      ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
      verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(Void.class));

      assertThat(captor.getValue().getHeaders().getFirst("Authorization"))
              .isEqualTo("Bearer mock-token");
    }

    @Test
    @DisplayName("does not throw when notification service is unavailable")
    void shouldNotThrowWhenNotificationServiceFails() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenThrow(new RuntimeException("Service unavailable"));

      // Exception is swallowed inside the method — registration should not break
      assertThatCode(() -> authService.sendPasswordResetNotification(EMAIL))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sends the correct email in the request body")
    void shouldSendCorrectEmailInBody() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenReturn(ResponseEntity.ok().build());

      authService.sendPasswordResetNotification(EMAIL);

      ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
      verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(Void.class));

      PasswordResetRequestDto body = (PasswordResetRequestDto) captor.getValue().getBody();
      Assertions.assertNotNull(body);
      assertThat(body.getEmail()).isEqualTo(EMAIL);
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // sendEmailVerificationNotification()
  // ═══════════════════════════════════════════════════════════════

  @Nested
  @DisplayName("sendEmailVerificationNotification()")
  class SendEmailVerificationNotification {

    @BeforeEach
    void stubJwt() {
      Jwt jwt = mock(Jwt.class);
      when(jwt.getTokenValue()).thenReturn("mock-token");
      lenient().when(jwtAuthenticationConverter.getCurrentJwt()).thenReturn(jwt);
    }

    @Test
    @DisplayName("sends POST to email-verification notification endpoint")
    void shouldPostToEmailVerificationEndpoint() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenReturn(ResponseEntity.ok().build());

      authService.sendEmailVerificationNotification(EMAIL);

      ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
      verify(restTemplate).postForEntity(urlCaptor.capture(), any(HttpEntity.class), eq(Void.class));

      assertThat(urlCaptor.getValue()).contains("email-verification");
    }

    @Test
    @DisplayName("sends request with Bearer token Authorization header")
    void shouldSendBearerTokenInHeader() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenReturn(ResponseEntity.ok().build());

      authService.sendEmailVerificationNotification(EMAIL);

      ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
      verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(Void.class));

      assertThat(captor.getValue().getHeaders().getFirst("Authorization"))
              .isEqualTo("Bearer mock-token");
    }

    @Test
    @DisplayName("does not throw when notification service is unavailable")
    void shouldNotThrowWhenNotificationServiceFails() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenThrow(new RuntimeException("Service unavailable"));

      assertThatCode(() -> authService.sendEmailVerificationNotification(EMAIL))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sends the correct email in the request body")
    void shouldSendCorrectEmailInBody() {
      when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
              .thenReturn(ResponseEntity.ok().build());

      authService.sendEmailVerificationNotification(EMAIL);

      ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
      verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(Void.class));

      PasswordResetRequestDto body = (PasswordResetRequestDto) captor.getValue().getBody();
      Assertions.assertNotNull(body);
      assertThat(body.getEmail()).isEqualTo(EMAIL);
    }
  }
}