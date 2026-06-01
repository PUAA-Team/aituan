package com.aituan.interaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

record ReviewCreateRequest(
    @Min(1) @Max(5) Integer rating,
    @NotBlank String content,
    List<@NotBlank String> labels,
    List<String> imageUrls) {}

record ReviewReportRequest(
    @NotBlank String reason,
    String detail) {}

record MerchantReviewReplyRequest(
    @NotBlank String content) {}

record AdminReviewAuditRequest(
    @NotBlank String action,
    String remark) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ReviewView(
    Long id,
    Long orderId,
    String orderNo,
    String orderTitle,
    Long storeId,
    String storeName,
    Integer rating,
    String content,
    List<String> labels,
    List<String> imageUrls,
    Integer helpfulCount,
    Integer reportedCount,
    Boolean helpfulByMe,
    String status,
    boolean replied,
    String replyContent,
    LocalDateTime repliedAt,
    LocalDateTime createdAt,
    String userMaskedNickname,
    List<String> reportReasons) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ReviewHelpfulView(boolean helpful, int helpfulCount) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ReviewReportView(Long reportId, String status) {}
