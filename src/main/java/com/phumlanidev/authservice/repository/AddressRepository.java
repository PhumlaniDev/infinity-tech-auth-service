package com.phumlanidev.authservice.repository;


import com.phumlanidev.authservice.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Comment: this is the placeholder for documentation.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
}
