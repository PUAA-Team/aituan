package com.aituan.identity.admin;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

record AdminProfileView(
    Long accountId,
    String accountNo,
    String accountType,
    String nickname,
    String phone,
    String email,
    String status,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt) {}

record AdminUserView(
    Long accountId,
    Long userId,
    String nickname,
    String avatarUrl,
    String phone,
    String email,
    String status,
    Long addressCount,
    Long orderCount,
    LocalDateTime createdAt) {}

record AdminStatusRequest(@NotBlank String status) {}

record AdminUserUpdateRequest(
    @NotBlank String nickname,
    String phone,
    String email,
    String avatarUrl,
    String status) {}
