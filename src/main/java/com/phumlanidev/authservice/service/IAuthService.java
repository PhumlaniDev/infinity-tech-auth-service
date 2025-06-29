package com.phumlanidev.authservice.service;


import com.phumlanidev.authservice.dto.JwtResponseDto;
import com.phumlanidev.authservice.dto.LoginDto;
import com.phumlanidev.authservice.dto.TokenLogoutRequest;
import com.phumlanidev.authservice.dto.UserDto;

public interface IAuthService {

  void registerUser(UserDto userDto);

  JwtResponseDto login(LoginDto loginDto);

  void logout(TokenLogoutRequest refreshToken);

  void sendEmailVerificationNotification(String email);

  void sendPasswordResetNotification(String email);
}
