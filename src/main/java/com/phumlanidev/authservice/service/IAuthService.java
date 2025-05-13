package com.phumlanidev.authservice.service;


import com.phumlanidev.authservice.dto.JwtResponseDto;
import com.phumlanidev.authservice.dto.LoginDto;
import com.phumlanidev.authservice.dto.TokenLogoutRequest;
import com.phumlanidev.authservice.dto.UserDto;

/**
 * Comment: this is the placeholder for documentation.
 */
public interface IAuthService {

  /**
   * Comment: this is the placeholder for documentation.
   */
  void registerUser(UserDto userDto);

  /**
   * Comment: this is the placeholder for documentation.
   */
  JwtResponseDto login(LoginDto loginDto);

  /**
   * Comment: this is the placeholder for documentation.
   */
  void logout(TokenLogoutRequest refreshToken);

  /**
   * Comment: this is the placeholder for documentation.
   */
  void sendResetPasswordEmail(String email);
}
