package com.phumlanidev.authservice.mapper;


import com.phumlanidev.authservice.dto.UserDto;
import com.phumlanidev.authservice.model.User;
import org.springframework.stereotype.Component;

/**
 * Comment: this is the placeholder for documentation.
 */
@Component
public class UserMapper {

  /**
   * Comment: this is the placeholder for documentation.
   */
  public User toEntity(UserDto dto, User user) {
    user.setUsername(dto.getUsername());
    user.setEmail(dto.getEmail());
    user.setFirstName(dto.getFirstName());
    user.setLastName(dto.getLastName());
    user.setPhoneNumber(dto.getPhoneNumber());
    user.setPassword(dto.getPassword());
    user.setRole(dto.getRole());
    return user;

  }

  /**
   * Comment: this is the placeholder for documentation.
   */
  public UserDto toDto(User user, UserDto dto) {
    dto.setUsername(user.getUsername());
    dto.setEmail(user.getEmail());
    dto.setFirstName(user.getFirstName());
    dto.setLastName(user.getLastName());
    dto.setPhoneNumber(user.getPhoneNumber());
    dto.setRole(user.getRole());
    dto.setAddress(dto.getAddress());

    return dto;
  }
}
