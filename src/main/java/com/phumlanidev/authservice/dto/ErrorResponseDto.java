package com.phumlanidev.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Comment: this is the placeholder for documentation.
 */
@Data
@AllArgsConstructor
public class ErrorResponseDto {

  @NotBlank(message = "API path is required")
  private String apiPath;
  @NotNull(message = "Error code is required")
  private HttpStatus errorCode;
  @NotBlank(message = "Error message is required")
  private String errorMessage;
  @NotNull(message = "Error time is required")
  private LocalDateTime errorTime;
}
