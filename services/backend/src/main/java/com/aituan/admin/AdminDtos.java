package com.aituan.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

record AdminMerchantView(
    Long merchantId,
    Long accountId,
    String merchantName,
    String contactName,
    String contactPhone,
    String licenseNo,
    String status,
    String auditStatus,
    Long storeCount,
    Long itemCount,
    LocalDateTime settledAt) {}

record AdminMerchantApplicationView(
    Long id,
    String applicationNo,
    Long accountId,
    String merchantName,
    String contactName,
    String contactPhone,
    String businessType,
    String storeName,
    String address,
    String status,
    String auditRemark,
    LocalDateTime submittedAt,
    Long auditedBy,
    LocalDateTime auditedAt) {}

record AdminMerchantApplicationAuditRequest(String remark) {}

record AdminCertificationMaterialView(
    Long id,
    Long merchantId,
    Long applicationId,
    String merchantName,
    String materialType,
    String materialName,
    String fileUrl,
    String status,
    String rejectReason,
    LocalDateTime submittedAt,
    Long auditedBy,
    LocalDateTime auditedAt) {}

record AdminCertificationMaterialAuditRequest(
    @NotBlank String status,
    String rejectReason) {}

record AdminStoreView(
    Long storeId,
    Long merchantId,
    String merchantName,
    String storeName,
    String businessType,
    String summary,
    String address,
    String status,
    String businessHoursText,
    String tagText,
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

record AdminMerchantUpsertRequest(
    Long accountId,
    @NotBlank String merchantName,
    String contactName,
    String contactPhone,
    String licenseNo,
    String status,
    String auditStatus) {}

record AdminStoreUpsertRequest(
    @NotNull Long merchantId,
    @NotBlank String storeName,
    @NotBlank String businessType,
    @NotBlank String summary,
    @NotBlank String address,
    String status,
    String businessHoursText,
    String tagText,
    String coverUrl,
    String contactPhone,
    String announcement) {}

record AdminUserUpdateRequest(
    @NotBlank String nickname,
    String phone,
    String email,
    String avatarUrl,
    String status) {}

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
