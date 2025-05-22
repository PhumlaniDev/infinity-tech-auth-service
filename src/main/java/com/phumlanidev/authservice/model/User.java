package com.phumlanidev.authservice.model;


import com.phumlanidev.authservice.enums.RoleMapping;
import jakarta.persistence.*;
import lombok.*;

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
