package com.phumlanidev.infinity_tech_auth_service.exception.auth;


import com.phumlanidev.infinity_tech_auth_service.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Comment: this is the placeholder for documentation.
 */
public class MissingRoleException extends BaseException {

  /**
   * Comment: this is the placeholder for documentation.
   */
  public MissingRoleException(String message) {
    super(message, HttpStatus.FORBIDDEN);
  }
}
