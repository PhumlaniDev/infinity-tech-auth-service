package com.phumlanidev.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Comment: this is the placeholder for documentation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {


  @NotBlank(message = "Username is required")
  private String username;
  @NotBlank(message = "Password is required")
  private String password;
}
