package com.phumlanidev.authservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenLogoutRequest {

    private String refreshToken;
}
