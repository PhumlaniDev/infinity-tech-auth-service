package com.phumlanidev.infinity_tech_auth_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Comment: this is the placeholder for documentation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  public static final String ADMIN = "admin";
  public static final String USER = "user";
  private final JwtAuthConverter jwtAuthConverter;

  /**
   * Comment: this is the placeholder for documentation.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Comment: this is the placeholder for documentation.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(
            authorize -> authorize
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/products/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/admin/**")
                .hasRole(ADMIN).anyRequest()
                .authenticated()).oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
        .sessionManagement(
            session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
  }
}
