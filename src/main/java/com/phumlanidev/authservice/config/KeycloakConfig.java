package com.phumlanidev.authservice.config;


import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Comment: this is the placeholder for documentation.
 */
@Slf4j
@Configuration
public class KeycloakConfig {

  @Value("${keycloak.auth-server-url}")
  private String keycloakServerUrl;

  @Value("${keycloak.realm}")
  private String keycloakRealm;

  // auth-service client (service-to-service)
  @Value("${keycloak.resource}")
  private String keycloakClientId;

  @Value("${keycloak.credentials.secret}")
  private String keycloakClientSecret;

  // auth-service-admin client (admin actions)
  @Value("${keycloak.admin.client-id}")
  private String keycloakAdminClientId;

  @Value("${keycloak.admin.client-secret}")
  private String keycloakAdminClientSecret;

  /**
   * Keycloak instance for admin actions.
   */
  @Bean
  @Primary
  public Keycloak keycloak() {
    log.info("Configuring Keycloak Admin Client...");
    return KeycloakBuilder.builder()
            .serverUrl(keycloakServerUrl)
            .realm(keycloakRealm)
            .clientId(keycloakAdminClientId)
            .clientSecret(keycloakAdminClientSecret)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .build();
  }

  /**
   * Keycloak instance for service client actions.
   */
  @Bean
  @Qualifier("keycloakServiceClient")
  public Keycloak keycloakServiceClient() {
    log.info("Configuring Keycloak Service Client...");
    return KeycloakBuilder.builder()
      .serverUrl(keycloakServerUrl)
      .realm(keycloakRealm)
      .clientId(keycloakClientId)
      .clientSecret(keycloakClientSecret)
      .grantType(OAuth2Constants.CLIENT_CREDENTIALS) // Service-to-service authentication
      .build();
  }
}
