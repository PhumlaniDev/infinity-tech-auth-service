package com.phumlanidev.infinity_tech_auth_service.controller;


import com.phumlanidev.infinity_tech_auth_service.config.JwtResponse;
import com.phumlanidev.infinity_tech_auth_service.constant.Constant;
import com.phumlanidev.infinity_tech_auth_service.dto.LoginDto;
import com.phumlanidev.infinity_tech_auth_service.dto.ResponseDto;
import com.phumlanidev.infinity_tech_auth_service.dto.UserDto;
import com.phumlanidev.infinity_tech_auth_service.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comment: this is the placeholder for documentation.
 */
@RestController
@RequestMapping("/api/v1/auth/")
@RequiredArgsConstructor
public class AuthController {

  /**
   * Comment: this is the placeholder for documentation.
   */

  private final AuthServiceImpl authServiceImpl;

  /**
   * Comment: this is the placeholder for documentation.
   */
  @PostMapping("/register")
  public ResponseEntity<ResponseDto> register(@Valid @RequestBody UserDto userDto) {
    authServiceImpl.registerUser(userDto);
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new ResponseDto(Constant.STATUS_CODE_CREATED,
                    "You have successfully Registered."));
  }

  /**
   * Comment: this is the placeholder for documentation.
   */
  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginDto loginDto) {
    String token = authServiceImpl.login(loginDto);
    return ResponseEntity.ok(new JwtResponse(token));
  }
}
