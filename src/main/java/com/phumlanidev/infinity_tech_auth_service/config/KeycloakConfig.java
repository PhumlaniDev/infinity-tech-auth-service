package com.phumlanidev.infinity_tech_auth_service.config;


import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Comment: this is the placeholder for documentation.
 */
@Configuration
@RequiredArgsConstructor
public class KeycloakConfig {

  private static final Logger logger = LoggerFactory.getLogger(KeycloakConfig.class);

  @Value("${keycloak.auth-server-url}")
  private final String keycloakServerUrl;
  @Value("${keycloak.realm}")
  private final String keycloakRealm;
  @Value("${keycloak.resource}")
  private final String keycloakClientId;
  @Value("${keycloak.credentials.secret}")
  private final String keycloakClientSecret;
  @Value("${keycloak.admin.username}")
  private final String keycloakClientAdminUsername;
  @Value("${keycloak.admin.password}")
  private final String keycloakClientAdminPassword;

  /**
   * Keycloak instance for admin actions.
   */
  @Bean
  public Keycloak keycloak() {
    logger.info("Configuring Keycloak Admin Client...");
    return KeycloakBuilder.builder()
            .serverUrl(keycloakServerUrl)
            .realm(keycloakRealm)
            .clientId(keycloakClientId)
            .clientSecret(keycloakClientSecret)
            .username(keycloakClientAdminUsername)
            .password(keycloakClientAdminPassword)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .build();
  }

  /**
   * Keycloak instance for service client actions.
   */
  @Bean
  public Keycloak keycloakServiceClient() {
    logger.info("Configuring Keycloak Service Client...");
    return KeycloakBuilder.builder()
      .serverUrl(keycloakServerUrl)
      .realm(keycloakRealm)
      .clientId(keycloakClientId)
      .clientSecret(keycloakClientSecret)
      .grantType(OAuth2Constants.CLIENT_CREDENTIALS) // Service-to-service authentication
      .build();
  }
}
