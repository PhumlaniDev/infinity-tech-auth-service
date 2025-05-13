package com.phumlanidev.authservice.enums;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;

/**
 * Comment: this is the placeholder for documentation.
 */
@Getter
public enum RoleMapping {

  ADMIN("admin", "client_admin"), USER("user", "client_user");

  private final String realmRole;
  private final String clientRole;

  RoleMapping(String realmRole, String clientRole) {
    this.realmRole = realmRole;
    this.clientRole = clientRole;
  }

  /**
   * Comment: this is the placeholder for documentation.
   */
  public static Optional<RoleMapping> from(String role) {
    return Stream.of(values()).filter(mapping -> mapping.name().equalsIgnoreCase(role)).findFirst();
  }

}
