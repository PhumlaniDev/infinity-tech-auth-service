package com.phumlanidev.infinity_tech_auth_service.repository;

import com.phumlanidev.infinity_tech_auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Comment: this is the placeholder for documentation.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Comment: this is the placeholder for documentation.
   */
  User findByUsername(String username);
}
