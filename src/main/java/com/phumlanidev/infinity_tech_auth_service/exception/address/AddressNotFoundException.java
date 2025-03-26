package com.phumlanidev.infinity_tech_auth_service.exception.address;

import com.phumlanidev.techhivestore.exception.BaseException;
import org.springframework.http.HttpStatus;


/**
 * Comment: this is the placeholder for documentation.
 */
public class AddressNotFoundException extends BaseException {

  /**
   * Comment: this is the placeholder for documentation.
   */
  public AddressNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }
}
