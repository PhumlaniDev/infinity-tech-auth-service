package com.phumlanidev.authservice.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class KeycloakJwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Value("${keycloak.principle-attribute:preferred_username}")
  private String principleAttribute;
  @Value("${keycloak.resource}")
  private String resourceId;

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
    Set<GrantedAuthority> authorities = new HashSet<>(rolesFrom(jwt.getClaimAsMap("realm_access")));

    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
    if (resourceAccess != null && resourceAccess.get(resourceId) instanceof Map<?,?> client) {
      authorities.addAll(rolesFrom(client));
    }

    log.debug("🔐 Extracted {} authorities from token subject: {}", authorities.size(), jwt.getSubject());

    return new JwtAuthenticationToken(jwt, authorities, principalName(jwt));
  }

  private String principalName(Jwt jwt) {
    String name = jwt.getClaimAsString(principleAttribute);
    return name != null ? name : jwt.getSubject();
  }

  private Set<SimpleGrantedAuthority> rolesFrom(Map<?,?> source) {
    if (source == null || !(source.get("roles") instanceof Collection<?> roles)) {
      return Set.of();
    }
    return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toSet());
  }
}