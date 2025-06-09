package com.phumlanidev.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Comment: this is the placeholder for documentation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDto {

  @NotBlank(message = "Access token is required")
  private String accessToken;
  @NotBlank(message = "Token type is required")
  private String refreshToken;
  @NotNull(message = "User profile is required")
  private Long expiresIn;
}