package com.aituan.engagementplatform.platform;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

record AnnouncementView(Long id, String title, String content, String targetClient, String coverUrl,
                        String status, LocalDateTime startAt, LocalDateTime endAt, Integer sortOrder,
                        Long createdBy, LocalDateTime updatedAt) {}
record AnnouncementUpsertRequest(@NotBlank String title, @NotBlank String content, String targetClient,
                                 String coverUrl, String status, LocalDateTime startAt,
                                 LocalDateTime endAt, Integer sortOrder) {}
record StatusRequest(@NotBlank String status) {}
record ConfigView(String configKey, String configValue, String remark, LocalDateTime updatedAt) {}
record ConfigUpdateRequest(@NotBlank String configValue, String remark) {}
record DeliverySettingView(Boolean autoAdvanceEnabled, Integer tickMinutes) {}
record DeliverySettingRequest(Boolean autoAdvanceEnabled, Integer tickMinutes) {}
record AuditLogView(Long id, String actorType, Long actorId, String actionType, String targetType,
                    Long targetId, String detail, String callerService, LocalDateTime createdAt) {}
record DashboardView(Map<String, Object> users, Map<String, Object> merchants,
                     Map<String, Object> orders, Map<String, Long> governance,
                     boolean degraded) {}
record ReviewSummaryView(BigDecimal rating, long count, List<String> highlights) {}
record StoreEngagementView(BigDecimal rating, long reviewCount,
                           long pendingReplyCount, long activeSessionCount) {}
record InternalAuditLogRequest(@NotBlank String actorType, Long actorId, @NotBlank String actionType,
                               @NotBlank String targetType, Long targetId, String detail) {}
record InternalAuditLogView(long id, boolean duplicate) {}
