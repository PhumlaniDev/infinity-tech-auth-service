package com.phumlanidev.authservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@TestConfiguration
public class TestSecurityConfig {

  @Bean
  @Primary
  public JwtDecoder jwtDecoder() {
    // Returns a decoder that always throws — tests mock IAuthService
    // directly so no real JWT validation is needed
    return token -> {
      throw new JwtException("JWT validation disabled in tests");
    };
  }
}
