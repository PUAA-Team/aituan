package com.aituan.identity.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

record EmailCodeRequest(@NotBlank @Email String email, @NotBlank String scene) {}

record EmailCodeResponse(String email, String scene, String code, long expireSeconds) {}

record RegisterRequest(
    @NotBlank @Pattern(regexp = "^\\d{11}$", message = "手机号必须是 11 位数字") String phone,
    @NotBlank @Email String email,
    @NotBlank String emailCode,
    @NotBlank String password) {}

record LoginRequest(@NotBlank String account, @NotBlank String password) {}

record ResetPasswordRequest(
    @NotBlank @Email String email,
    @NotBlank String emailCode,
    @NotBlank String newPassword) {}

record TokenCheckResponse(boolean valid, AuthProfile profile) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record AuthProfile(Long id, String nickname, String avatarUrl, String phone, String email, String memberLevelName) {}

record AuthResponse(String token, long expiresIn, AuthProfile profile) {}
