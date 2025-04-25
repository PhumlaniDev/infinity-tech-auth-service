package com.phumlanidev.auth_service.exception.auth;


import com.phumlanidev.auth_service.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Comment: this is the placeholder for documentation.
 */
public class UnauthorizedActionException extends BaseException {

  /**
   * Comment: this is the placeholder for documentation.
   */
  public UnauthorizedActionException(String message) {
    super(message, HttpStatus.FORBIDDEN);
  }
}
