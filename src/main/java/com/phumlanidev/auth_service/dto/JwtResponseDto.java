package com.phumlanidev.auth_service.dto;

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
  private String accessToken;
  private String refreshToken;
  private Long expiresIn;
}