package com.phumlanidev.infinity_tech_auth_service.dto;


import com.phumlanidev.infinity_tech_auth_service.enums.RoleMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
