package com.phumlanidev.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
  private String id;
  private String username;
  private String email;
  private boolean enabled;
}
