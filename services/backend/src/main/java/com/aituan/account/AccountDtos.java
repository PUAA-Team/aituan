package com.aituan.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

record AccountProfileView(
    Long accountId,
    Long userId,
    String nickname,
    String avatarUrl,
    String phone,
    String email,
    String memberLevelName,
    int growthValue,
    long addressCount,
    long favoriteCount,
    long orderCount,
    long unreadMessageCount) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record AddressView(
    Long id,
    String contactName,
    String contactPhone,
    String province,
    String city,
    String district,
    String detailAddress,
    Double longitude,
    Double latitude,
    String tagName,
    boolean isDefault,
    String deliveryNote,
    LocalDateTime createdAt) {}

record AddressUpsertRequest(
    @NotBlank String contactName,
    @NotBlank String contactPhone,
    @NotBlank String province,
    @NotBlank String city,
    @NotBlank String district,
    @NotBlank String detailAddress,
    Double longitude,
    Double latitude,
    String tagName,
    Boolean isDefault,
    String deliveryNote) {}

record AccountProfileUpdateRequest(
    @NotBlank String nickname,
    String avatarUrl) {}

record PasswordChangeRequest(
    @NotBlank String oldPassword,
    @NotBlank String newPassword) {}

record FavoriteUpsertRequest(
    @NotBlank String favoriteType,
    @NotNull Long targetId,
    @NotBlank String targetName,
    String coverUrl,
    String subtitle) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record FavoriteView(
    Long id,
    String favoriteType,
    Long targetId,
    String targetName,
    String coverUrl,
    String subtitle,
    LocalDateTime createdAt) {}
