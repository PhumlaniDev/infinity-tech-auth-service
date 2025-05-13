package com.phumlanidev.authservice.dto;

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
public class AddressDto {

  private String streetName;
  private String city;
  private String province;
  private String zipCode;
  private String country;
}
