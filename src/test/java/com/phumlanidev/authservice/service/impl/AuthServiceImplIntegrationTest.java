package com.phumlanidev.authservice.service.impl;


import com.phumlanidev.authservice.config.JwtAuthenticationConverter;
import com.phumlanidev.authservice.dto.AddressDto;
import com.phumlanidev.authservice.dto.UserDto;
import com.phumlanidev.authservice.enums.RoleMapping;
import com.phumlanidev.authservice.helper.KeycloakAdminHelper;
import com.phumlanidev.authservice.model.User;
import com.phumlanidev.authservice.repository.AddressRepository;
import com.phumlanidev.authservice.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthServiceImplIntegrationTest {

  @Container
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")
          .withDatabaseName("auth_db_test")
          .withUsername("auth_test_user")
          .withPassword("auth_test_password");

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    registry.add("eureka.client.enabled", () -> "false");
    registry.add("eureka.client.register-with-eureka", () -> "false");
    registry.add("eureka.client.fetch-registry", () -> "false");
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
      return token -> Jwt.withTokenValue(token)
              .header("alg", "none")
              .claim("sub", "test-user")
              .claim("preferred_username", "phumlani")
              .issuedAt(Instant.now())
              .expiresAt(Instant.now().plusSeconds(3600))
              .build();
    }
  }

  @MockitoBean
  private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockitoBean
  private AuditLogServiceImpl auditLogService;
  @MockitoBean
  private KeycloakAdminHelper keycloakAdminHelper;
  @MockitoBean
  private RestTemplate restTemplate;
  @Autowired
  private AuthServiceImpl authService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private AddressRepository addressRepository;


  private static final String USER_ID = "test-user-id";
  private static final String USERNAME = "phumlani";
  private static final String CLIENT_IP = "127.0.0.1";

  private UserDto validUserDto() {
    AddressDto addressDto = AddressDto.builder()
            .streetName("123 Main St")
            .city("Cape Town")
            .province("western cape")
            .zipCode("8000")
            .country("South Africa")
            .build();

    return UserDto.builder()
            .firstName("Phumlani")
            .lastName("Arendse")
            .username(USERNAME)
            .email("example@example.com")
            .password("Password123!")
            .address(addressDto)
            .role(RoleMapping.USER)
            .phoneNumber("071234567")
            .build();
  }

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    addressRepository.deleteAll();

    lenient().when(jwtAuthenticationConverter.getCurrentUserId()).thenReturn(USER_ID);
    lenient().when(jwtAuthenticationConverter.getCurrentUsername()).thenReturn(USERNAME);
    lenient().when(jwtAuthenticationConverter.getCurrentJwt()).thenReturn(
            Jwt.withTokenValue("mock-token")
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .claim("preferred_username", "phumlani")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build()
    );
    lenient().when(restTemplate.postForEntity(anyString(), any(HttpClient.class), eq(Void.class)))
            .thenReturn(ResponseEntity.ok().build());
  }

  @Nested
  @DisplayName("registerUser()")
  class RegisterUser {

    @Test
    @DisplayName("persist user and address to the database")
    void shouldPersistUserAndAddress() {
      authService.registerUser(validUserDto());

      Optional<User> savedUser = Optional.ofNullable(userRepository.findByUsername(USERNAME));
      assertThat(savedUser).isPresent();
      assertThat(savedUser.get().getUsername()).isEqualTo(USERNAME);
      assertThat(savedUser.get().getEmail()).isEqualTo("example@example.com");
    }
  }
}
