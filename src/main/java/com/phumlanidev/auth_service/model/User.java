package com.phumlanidev.auth_service.model;


import com.phumlanidev.auth_service.enums.RoleMapping;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Comment: this is the placeholder for documentation.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long userId;
  @Column(unique = true)
  private String username;
  private String password;
  @Column(unique = true)
  private String email;
  private String firstName;
  private String lastName;
  private String phoneNumber;
  @Enumerated(EnumType.STRING)
  private RoleMapping role;
  @ManyToOne
  @JoinColumn(name = "address_id", referencedColumnName = "addressId")
  private Address address;

}
