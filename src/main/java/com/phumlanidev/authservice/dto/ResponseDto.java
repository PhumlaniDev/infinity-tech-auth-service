package com.phumlanidev.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Comment: this is the placeholder for documentation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDto {

  @NotBlank(message = "Status code is required")
  private String statusCode;
  @NotBlank(message = "Status message is required")
  private String statusMsg;
}
