package com.phumlanidev.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditLogDto {

    @NotBlank(message = "ID cannot be blank")
    private String id;
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    @NotBlank(message = "Username cannot be blank")
    private String username;
    @NotBlank(message = "Action cannot be blank")
    private String action;
    @NotBlank(message = "IP address cannot be blank")
    private String ip;
    @NotBlank(message = "Details cannot be blank")
    private String details;
    @NotNull(message = "Timestamp cannot be null")
    private Instant timestamp;
}
