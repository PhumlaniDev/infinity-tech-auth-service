package com.phumlanidev.infinity_tech_auth_service.service;


import com.phumlanidev.infinity_tech_auth_service.dto.LoginDto;
import com.phumlanidev.infinity_tech_auth_service.dto.UserDto;

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
  String login(LoginDto loginDto);

}
