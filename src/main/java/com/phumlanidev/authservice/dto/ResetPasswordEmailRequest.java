package com.phumlanidev.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ResetPasswordEmailRequest {

  private String email;
  private String resetLink;
}
