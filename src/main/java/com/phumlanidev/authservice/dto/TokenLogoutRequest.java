package com.phumlanidev.authservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenLogoutRequest {

    @NotNull(message = "Access token is required")
    private String refreshToken;
}
