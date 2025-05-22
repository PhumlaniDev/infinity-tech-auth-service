package com.phumlanidev.authservice.dto;


import com.phumlanidev.authservice.enums.RoleMapping;
import lombok.*;

/**
 * Comment: this is the placeholder for documentation.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

  private String firstName;
  private String lastName;
  private String username;
  private String email;
  private String password;
  private String phoneNumber;
  private RoleMapping role;
  private AddressDto address;
}
