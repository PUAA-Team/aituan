package com.aituan.admin;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

record AdminDashboardView(
    Long todayOrders,
    BigDecimal todayAmount,
    Long abnormalOrders,
    Long merchantCount,
    Long userCount,
    Long itemCount,
    Long deliveringTasks) {}

record AdminMerchantView(
    Long merchantId,
    String merchantName,
    String contactName,
    String contactPhone,
    String status,
    String auditStatus,
    Long storeCount,
    Long itemCount,
    LocalDateTime settledAt) {}

record AdminStoreView(
    Long storeId,
    Long merchantId,
    String merchantName,
    String storeName,
    String businessType,
    String summary,
    String address,
    String status,
    String coverUrl,
    String contactPhone,
    String announcement,
    LocalDateTime updatedAt) {}

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

record AdminDeliveryTaskView(
    Long taskId,
    Long orderId,
    String orderNo,
    String storeName,
    String currentStage,
    String currentStageText,
    Boolean autoAdvanceEnabled,
    LocalDateTime pausedAt,
    String abnormalReason,
    LocalDateTime nextTickAt,
    LocalDateTime completedAt,
    LocalDateTime updatedAt) {}

record AdminDeliveryActionRequest(String remark, String reason) {}

record AdminDeliverySettingView(Boolean autoAdvanceEnabled, Integer tickMinutes) {}

record AdminDeliverySettingRequest(Boolean autoAdvanceEnabled, Integer tickMinutes) {}

record AdminAnnouncementView(
    Long id,
    String title,
    String content,
    String targetClient,
    String coverUrl,
    String status,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Integer sortOrder,
    Long createdBy,
    LocalDateTime updatedAt) {}

record AdminAnnouncementUpsertRequest(
    @NotBlank String title,
    @NotBlank String content,
    String targetClient,
    String coverUrl,
    String status,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Integer sortOrder) {}

record AdminConfigView(String configKey, String configValue, String remark, LocalDateTime updatedAt) {}

record AdminConfigUpdateRequest(@NotBlank String configValue, String remark) {}

record AdminAuditLogView(
    Long id,
    String actorType,
    Long actorId,
    String actionType,
    String targetType,
    Long targetId,
    String detail,
    LocalDateTime createdAt) {}
