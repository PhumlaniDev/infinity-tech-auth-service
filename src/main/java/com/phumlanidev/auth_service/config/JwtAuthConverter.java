package com.phumlanidev.auth_service.config;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;


/**
 * Comment: this is the placeholder for documentation.
 */
@Slf4j
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
      new JwtGrantedAuthoritiesConverter();

  @Value("${keycloak.principle-attribute}")
  private String principleAttribute;
  @Value("${keycloak.resource}")
  private String resourceId;

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {

    Collection<GrantedAuthority> authorities = extractAllRoles(jwt);

    return new JwtAuthenticationToken(jwt, authorities, getPrincipleClaimName(jwt));
  }

  private String getPrincipleClaimName(Jwt jwt) {
    return Optional.ofNullable(principleAttribute)
            .map(jwt::getClaim)
            .orElse(jwt.getClaim(JwtClaimNames.SUB)).toString();
  }

  private Collection<GrantedAuthority> extractAllRoles(Jwt jwt) {
    Set<String> roles = new HashSet<>();



    Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
    if (resourceAccess != null) {
      Map<String, Object> client = (Map<String, Object>) resourceAccess.get(resourceId);
      if (client != null) {
        Collection<String> clientRoles = (Collection<String>) client.get("roles");
        if (clientRoles != null) {
          roles.addAll(clientRoles);
        }
      }
    }

    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess != null) {
      Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
      if (realmRoles != null) {
        roles.addAll(realmRoles);
      }
    }

    Collection<String> topRoles = jwt.getClaim("roles");
    if (topRoles != null) {
      roles.addAll(topRoles);
    }

    log.info("Extracted roles: {}", roles);

    return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // Add "ROLE_" prefix
            .collect(Collectors.toSet());
  }
}
