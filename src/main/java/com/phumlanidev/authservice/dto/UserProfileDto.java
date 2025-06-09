package com.phumlanidev.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {

  @NotBlank(message = "Username is required")
  private String firstName;
  @NotBlank(message = "Last name is required")
  private String lastName;
  @NotBlank(message = "Username is required")
  @Email(message = "Email should be valid")
  private String email;

  }
