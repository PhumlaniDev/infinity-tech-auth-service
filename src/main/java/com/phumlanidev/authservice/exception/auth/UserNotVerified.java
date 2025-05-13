package com.phumlanidev.authservice.exception.auth;

import com.phumlanidev.authservice.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UserNotVerified extends BaseException {
    public UserNotVerified(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
