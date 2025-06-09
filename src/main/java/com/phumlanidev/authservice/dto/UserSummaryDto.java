package com.phumlanidev.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {

  @NotBlank(message = "User ID is required")
  private String id;
  @NotBlank(message = "Username is required")
  private String username;
  @NotBlank(message = "Email is required")
  private String email;
  @NotNull(message = "Enabled status is required")
  private boolean enabled;
}
