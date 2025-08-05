package com.phumlanidev.authservice.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Comment: this is the placeholder for documentation.
 */

@Entity
@Table
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Address extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long addressId;
  private String streetName;
  private String city;
  private String province;
  private String zipCode;
  private String country;
}
