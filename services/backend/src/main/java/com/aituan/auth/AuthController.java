package com.aituan.auth;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/open/auth")
class AuthController {
  private final AuthService authService;

  AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/email-code")
  ApiResponse<EmailCodeResponse> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
    return ApiResponse.ok(authService.sendEmailCode(request));
  }

  @PostMapping("/user/register")
  ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.ok(authService.register(request));
  }

  @PostMapping("/user/login/password")
  ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }

  @PostMapping("/user/password/reset")
  ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ApiResponse.ok(null);
  }

  @GetMapping("/token/check")
  ApiResponse<TokenCheckResponse> checkToken(@RequestHeader(value = "Authorization", required = false) String authorization) {
    CurrentUser currentUser = CurrentUserContext.optional().orElse(null);
    return ApiResponse.ok(authService.checkToken(currentUser));
  }

  @PostMapping("/logout")
  ApiResponse<Void> logout() {
    return ApiResponse.ok(null);
  }
}
