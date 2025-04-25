package com.phumlanidev.auth_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenLogoutRequest {

    private String refreshToken;
}
