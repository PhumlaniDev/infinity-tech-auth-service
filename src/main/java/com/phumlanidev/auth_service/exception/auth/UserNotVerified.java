package com.phumlanidev.auth_service.exception.auth;

import com.phumlanidev.auth_service.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserNotVerified extends BaseException {
    public UserNotVerified(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
