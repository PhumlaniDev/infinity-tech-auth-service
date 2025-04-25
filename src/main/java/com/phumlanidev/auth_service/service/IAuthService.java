package com.phumlanidev.auth_service.service;


import com.phumlanidev.auth_service.dto.JwtResponseDto;
import com.phumlanidev.auth_service.dto.LoginDto;
import com.phumlanidev.auth_service.dto.TokenLogoutRequest;
import com.phumlanidev.auth_service.dto.UserDto;
import jakarta.validation.Valid;

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

  void logout(TokenLogoutRequest refreshToken);
}
