package com.phumlanidev.authservice.controller;


import com.phumlanidev.authservice.constant.Constant;
import com.phumlanidev.authservice.dto.*;
import com.phumlanidev.authservice.service.IAuthService;
import com.phumlanidev.authservice.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/")
@RequiredArgsConstructor
public class AuthController {


  private final IAuthService authService;


  @PostMapping("/register")
  public ResponseEntity<ResponseDto> register(@Valid @RequestBody UserDto userDto) {
    authService.registerUser(userDto);
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new ResponseDto(Constant.STATUS_CODE_CREATED,
                    "You have successfully Registered."));
  }


  @PostMapping("/login")
  public ResponseEntity<JwtResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
    JwtResponseDto jwtResponse = authService.login(loginDto);
    return ResponseEntity.ok(jwtResponse);
  }


  @PostMapping("/logout")
  public ResponseEntity<ResponseDto> logout(@Valid @RequestBody TokenLogoutRequest refreshToken) {
    authService.logout(refreshToken);
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ResponseDto(Constant.STATUS_CODE_OK,
                    "You have successfully logged out."));
  }


  @PostMapping("/reset-password")
  public ResponseEntity<ResponseDto> resetPassword(@Valid @RequestBody PasswordResetRequestDto dto) {
    authService.sendPasswordResetNotification(dto.getEmail());
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ResponseDto(Constant.STATUS_CODE_OK,
                    "Reset password email sent successfully."));
  }

  @PostMapping("/verify-email")
  public ResponseEntity<ResponseDto> verifyEmail(@Valid @RequestBody EmailVerificationRequestDto dto) {
    authService.sendEmailVerificationNotification(dto.getEmail());
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ResponseDto(Constant.STATUS_CODE_OK,
                    "Email verification sent successfully."));
  }
}
