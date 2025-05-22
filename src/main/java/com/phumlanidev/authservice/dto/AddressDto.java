package com.phumlanidev.authservice.dto;

import lombok.*;

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
