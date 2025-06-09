package com.phumlanidev.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Comment: this is the placeholder for documentation.
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {

  @NotBlank(message = "Street name is required")
  private String streetName;
  @NotBlank(message = "Street number is required")
  private String city;
  @NotBlank(message = "City is required")
  private String province;
  @NotBlank(message = "Province is required")
  private String zipCode;
  @NotBlank(message = "Country is required")
  private String country;
}
