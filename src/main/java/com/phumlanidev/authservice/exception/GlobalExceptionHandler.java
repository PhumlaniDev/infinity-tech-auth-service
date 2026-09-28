package com.phumlanidev.authservice.exception;


import com.phumlanidev.authservice.dto.ErrorResponseDto;
import com.phumlanidev.authservice.exception.auth.AuthenticationFailedException;
import com.phumlanidev.authservice.exception.auth.KeycloakCommunicationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Comment: this is the placeholder for documentation.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  // ── Validation — overrides ResponseEntityExceptionHandler ────────────────
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
          MethodArgumentNotValidException ex,
          HttpHeaders headers,
          HttpStatusCode status,
          WebRequest request) {

    Map<String, String> validationErrors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String message = error.getDefaultMessage();
      validationErrors.put(fieldName, message);
    });

    return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
  }

  // ── Domain exceptions ─────────────────────────────────────────────────────

  @ExceptionHandler(UserAlreadyExistException.class)
  public ResponseEntity<ErrorResponseDto> handleUserAlreadyExistException(
          UserAlreadyExistException ex, WebRequest request) {
    log.warn("User already exists: {}", ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.CONFLICT)                        // ← 409 not 400
            .body(new ErrorResponseDto(
                    request.getDescription(false),
                    HttpStatus.CONFLICT,
                    ex.getMessage(),
                    LocalDateTime.now()
            ));
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleUserNotFoundException(
          UserNotFoundException ex, WebRequest request) {
    log.warn("User not found: {}", ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponseDto(
                    request.getDescription(false),
                    HttpStatus.NOT_FOUND,
                    ex.getMessage(),
                    LocalDateTime.now()
            ));
  }

  @ExceptionHandler(AuthenticationFailedException.class)
  public ResponseEntity<ErrorResponseDto> handleAuthenticationFailedException(
          AuthenticationFailedException ex, WebRequest request) {
    log.warn("Authentication failed: {}", ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponseDto(
                    request.getDescription(false),
                    HttpStatus.UNAUTHORIZED,
                    ex.getMessage(),
                    LocalDateTime.now()
            ));
  }

  @ExceptionHandler(KeycloakCommunicationException.class)
  public ResponseEntity<ErrorResponseDto> handleKeycloakCommunicationException(
          KeycloakCommunicationException ex, WebRequest request) {
    log.error("Keycloak communication failed: {}", ex.getMessage());
    return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponseDto(
                    request.getDescription(false),
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication service temporarily unavailable",
                    LocalDateTime.now()
            ));
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponseDto> handleBaseException(
          BaseException ex, WebRequest request) {
    log.warn("Base exception: {}", ex.getMessage());
    return ResponseEntity
            .status(ex.getStatus())
            .body(new ErrorResponseDto(
                    request.getDescription(false),
                    ex.getStatus(),
                    ex.getMessage(),
                    LocalDateTime.now()
            ));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
          AccessDeniedException ex, HttpServletRequest request) {
    log.warn("Access denied for URI: {}", request.getRequestURI());
    return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponseDto(
                    "uri=" + request.getRequestURI(),
                    HttpStatus.FORBIDDEN,
                    "Access is denied",
                    LocalDateTime.now()
            ));
  }

  // ── Catch-all — must be last ───────────────────────────────────────────────
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleGenericException(
          Exception ex, WebRequest request) {
    log.error("Unhandled exception: {}", ex.getMessage(), ex);

    // AccessDeniedException falls through here if not caught above
    // in some Spring Security configurations — guard it explicitly
    if (ex instanceof AccessDeniedException) {
      return ResponseEntity
              .status(HttpStatus.FORBIDDEN)
              .body(new ErrorResponseDto(
                      request.getDescription(false),
                      HttpStatus.FORBIDDEN,
                      "Access is denied",
                      LocalDateTime.now()
              ));
    }

    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponseDto(
                    request.getDescription(false),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred",
                    LocalDateTime.now()
            ));
  }
}