package com.phumlanidev.auth_service.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    Collection<GrantedAuthority> authorities = Stream.concat(
            Optional.of(jwtGrantedAuthoritiesConverter.convert(jwt))
                .orElse(Collections.emptyList()).stream(), extractResourceRoles(jwt).stream())
        .collect(Collectors.toSet());

    return new JwtAuthenticationToken(jwt, authorities, getPrincipleClaimName(jwt));
  }

  private String getPrincipleClaimName(Jwt jwt) {
    String claimName = JwtClaimNames.SUB;
    if (principleAttribute != null) {
      claimName = principleAttribute;
    }
    return jwt.getClaim(claimName);
  }

  private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
    Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
    Map<String, Object> resource;
    Collection<String> resourceRoles;

    if (resourceAccess == null ||
        (resource = (Map<String, Object>) resourceAccess.get(resourceId)) == null ||
        (resourceRoles = (Collection<String>) resource.get("roles")) == null) {
      return Set.of();
    }
    return resourceRoles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.toSet());
  }
}
