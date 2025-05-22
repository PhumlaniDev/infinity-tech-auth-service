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
  public ResponseEntity<JwtResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
    String accessToken = authServiceImpl.login(loginDto).getAccessToken();
    String refreshToken = authServiceImpl.login(loginDto).getRefreshToken();
    Long expiresIn = authServiceImpl.login(loginDto).getExpiresIn();
    return ResponseEntity.ok(new JwtResponseDto(accessToken, refreshToken, expiresIn));
  }

  /**
   * Comment: this is the placeholder for documentation.
   */
  @PostMapping("/logout")
  public ResponseEntity<ResponseDto> logout(@Valid @RequestBody TokenLogoutRequest refreshToken) {
    authServiceImpl.logout(refreshToken);
    return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ResponseDto(Constant.STATUS_CODE_OK,
                    "You have successfully logged out."));
  }

  /**
   * Comment: this is the placeholder for documentation.
   */
  @PostMapping("/reset-password")
    public ResponseEntity<ResponseDto> resetPassword(@RequestBody String email) {
        authServiceImpl.sendResetPasswordEmail(email);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto(Constant.STATUS_CODE_OK,
                        "Reset password email sent successfully."));
    }
}
