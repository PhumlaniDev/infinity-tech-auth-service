package com.phumlanidev.auth_service.repository;


import com.phumlanidev.auth_service.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Comment: this is the placeholder for documentation.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
}
