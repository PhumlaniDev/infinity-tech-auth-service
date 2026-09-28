package com.phumlanidev.authservice.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.phumlanidev.authservice.constant.Constant;
import com.phumlanidev.authservice.dto.AddressDto;
import com.phumlanidev.authservice.dto.JwtResponseDto;
import com.phumlanidev.authservice.dto.LoginDto;
import com.phumlanidev.authservice.dto.UserDto;
import com.phumlanidev.authservice.enums.RoleMapping;
import com.phumlanidev.authservice.exception.UserAlreadyExistException;
import com.phumlanidev.authservice.exception.auth.AuthenticationFailedException;
import com.phumlanidev.authservice.service.IAuthService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("integration-test")
public class AuthServiceImplIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private IAuthService authService;

  @MockitoBean
  private Keycloak keycloakAdminClient;
  @MockitoBean
  private Keycloak keycloakServiceClient;

  @Container
  static PostgreSQLContainer postgres =
          new PostgreSQLContainer("postgres:16-alpine")
          .withDatabaseName("auth_db_test")
          .withUsername("postgres")
          .withPassword("postgres");

  @RegisterExtension
  static WireMockExtension keycloak = WireMockExtension.newInstance()
          .options(wireMockConfig().port(9999))
          .build();

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("keycloak.auth-server-url",
            () -> "http://localhost:" + keycloak.getPort());
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            () -> "http://localhost:" + keycloak.getPort() + "/realms/ecommerce");

    registry.add("eureka.client.enabled", () -> "false");
    registry.add("eureka.client.register-with-eureka", () -> "false");
    registry.add("eureka.client.fetch-registry", () -> "false");
  }

  @BeforeEach
  void stubKeycloakOidcDiscovery() {
    keycloak.stubFor(get(urlEqualTo(
            "/realms/ecommerce/.well-known/openid-configuration"))
            .willReturn(okJson("""
                {
                  "issuer": "http://localhost:9999/realms/ecommerce",
                  "jwks_uri": "http://localhost:9999/realms/ecommerce/protocol/openid-connect/certs",
                  "token_endpoint": "http://localhost:9999/realms/ecommerce/protocol/openid-connect/token",
                  "subject_types_supported": ["public"]
                }
            """)));

    keycloak.stubFor(get(urlEqualTo(
            "/realms/ecommerce/protocol/openid-connect/certs"))
            .willReturn(okJson("""
                {
                  "keys": []
                }
            """)));
  }

  private UserDto validUserDto() {
    return UserDto.builder()
            .firstName("Phumlani")
            .lastName("Arendse")
            .username("phumlanidev")
            .email("aphumlani.dev@gmail.com")
            .password("Password123!")
            .address(AddressDto.builder()
                    .streetName("123 Main St")
                    .city("Cape Town")
                    .province("Western Cape")
                    .zipCode("8000")
                    .country("South Africa")
                    .build())
            .role(RoleMapping.USER)
            .phoneNumber("071234567")
            .build();
  }


  private static final String USER_ID = "test-user-id";
  private static final String USERNAME = "phumlani";
  private static final String CLIENT_IP = "127.0.0.1";

//  @BeforeEach
//  void setUp() {
//    userRepository.deleteAll();
//    addressRepository.deleteAll();
//
//    lenient().when(keycloakAdminHelper.getCurrentUserId()).thenReturn(USER_ID);
//    lenient().when(keycloakAdminHelper.getCurrentUsername()).thenReturn(USERNAME);
//    lenient().when(keycloakAdminHelper.getCurrentJwt()).thenReturn(
//            Jwt.withTokenValue("mock-token")
//                    .header("alg", "none")
//                    .claim("sub", "test-user")
//                    .claim("preferred_username", "phumlani")
//                    .issuedAt(Instant.now())
//                    .expiresAt(Instant.now().plusSeconds(3600))
//                    .build()
//    );
//    lenient().when(restTemplate.postForEntity(anyString(), any(HttpClient.class), eq(Void.class)))
//            .thenReturn(ResponseEntity.ok().build());
//  }

  @Nested
  @DisplayName("registerUser()")
  class RegisterUser {

    @Test
    @DisplayName("POST /register — valid request — 201 Created")
    void register_validRequest_returns201() throws Exception {
      doNothing().when(authService).registerUser(any(UserDto.class));

      mockMvc.perform(post("/api/v1/auth/register")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(validUserDto())))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.statusCode")
                      .value(Constant.STATUS_CODE_CREATED));  // ← code not message
//              .andExpect(jsonPath("$.statusMessage")
//                      .value("You have successfully registered."));
    }

    @Test
    @DisplayName("POST /register - missing required fields - 400 Bad Request")
    void register_missingFields_returns400() throws Exception {
      UserDto invalid = UserDto.builder()
              .firstName("Phumlani")
              .build();

      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalid)))
              .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register - invalid email - 400 Bad Request")
    void register_invalidEmail_returned400() throws Exception {
      UserDto invalidEmailUser = validUserDto();
      invalidEmailUser.setEmail("invalid-email");

      mockMvc.perform(post("/api/v1/auth/register")
              .contentType("application/json")
              .content(objectMapper.writeValueAsString(invalidEmailUser)))
              .andExpect(status().isBadRequest());

    }

    @Test
    @DisplayName("POST /register user already exists - 409 Conflict")
    void register_userAlreadyExists_return409() throws Exception {
      doThrow(new UserAlreadyExistException("User already exists"))
              .when(authService).registerUser(any());

      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(validUserDto())))
              .andExpect(status().isConflict());
    }
  }

  @Nested
  @DisplayName("loginUser")
  class LoginUser {

    @Test
    @DisplayName("POST /login - valid credentials - 200 with tokens")
    void login_validCredentials_returns200() throws Exception {
      LoginDto dto = LoginDto.builder()
              .username("phumlanidev")
              .password("SecurePass123!")
              .build();

      JwtResponseDto jwtResponseDto = new JwtResponseDto(
              "access-token-value",
              "refresh-token-value",
              900L
      );

      when(authService.login(any(LoginDto.class))).thenReturn(jwtResponseDto);

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(dto)))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.accessToken").value("access-token-value"))
              .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"))
              .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /login - invalid credentials - 401 Unauthorized")
    void login_invalidCredentials_returns401() throws Exception {
      LoginDto dto = LoginDto.builder()
              .username("phumlanidev")
              .password("wrongpass!")
              .build();

      when(authService.login(any()))
              .thenThrow(new AuthenticationFailedException(
                      "Invalid user or password"));

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(dto)))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /login - blank usernmae - 400 Bad Request")
    void login_blankUsername_return400() throws Exception {
      LoginDto dto = LoginDto.builder()
              .username("")
              .password("wrongpass!")
              .build();

      mockMvc.perform(post("/api/v1/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(dto)))
              .andExpect(status().isBadRequest());
    }
  }
}
