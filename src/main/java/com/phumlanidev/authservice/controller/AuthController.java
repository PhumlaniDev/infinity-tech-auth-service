package com.phumlanidev.authservice.controller;


import com.phumlanidev.authservice.constant.Constant;
import com.phumlanidev.authservice.dto.*;
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


  private final AuthServiceImpl authServiceImpl;


  @PostMapping("/register")
  public ResponseEntity<ResponseDto> register(@Valid @RequestBody UserDto userDto) {
    authServiceImpl.registerUser(userDto);
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new ResponseDto(Constant.STATUS_CODE_CREATED,
                    "You have successfully Registered."));
  }


  @PostMapping("/login")
  public ResponseEntity<JwtResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
    String accessToken = authServiceImpl.login(loginDto).getAccessToken();
    String refreshToken = authServiceImpl.login(loginDto).getRefreshToken();
    Long expiresIn = authServiceImpl.login(loginDto).getExpiresIn();
    return ResponseEntity.ok(new JwtResponseDto(accessToken, refreshToken, expiresIn));
  }


  @PostMapping("/logout")
  public ResponseEntity<ResponseDto> logout(@Valid @RequestBody TokenLogoutRequest refreshToken) {
    authServiceImpl.logout(refreshToken);
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ResponseDto(Constant.STATUS_CODE_OK,
                    "You have successfully logged out."));
  }


  @PostMapping("/reset-password")
  public ResponseEntity<ResponseDto> resetPassword(@Valid @RequestBody String email) {
    authServiceImpl.sendPasswordResetNotification(email);
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ResponseDto(Constant.STATUS_CODE_OK,
                    "Reset password email sent successfully."));
  }

  @PostMapping
  public ResponseEntity<ResponseDto> verifyEmail(@Valid @RequestBody String email) {
    authServiceImpl.sendEmailVerificationNotification(email);
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ResponseDto(Constant.STATUS_CODE_OK,
                    "Email verification sent successfully."));
  }
}
