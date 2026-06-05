package com.aituan.interaction;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.enums.DisplayOrderStatus;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InteractionService {
  private static final String REVIEW_STATUS_PUBLISHED = "published";
  private static final String REVIEW_STATUS_HIDDEN = "hidden";

  private final InteractionRepository interactionRepository;
  private final JdbcTemplate jdbcTemplate;

  InteractionService(InteractionRepository interactionRepository, JdbcTemplate jdbcTemplate) {
    this.interactionRepository = interactionRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  // ============ 用户端 ============

  PageResponse<ReviewView> myReviews(String statusFilter, int page, int pageSize) {
    long userId = requireUser().userId();
    String normalized = normalizeReviewStatus(statusFilter);
    long total = interactionRepository.countMyReviews(userId, normalized);
    List<ReviewView> list = interactionRepository.listMyReviews(userId, normalized, (page - 1) * pageSize, pageSize)
        .stream()
        .map(row -> toUserReviewView(row, userId))
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  ReviewView reviewDetail(long reviewId) {
    InteractionRepository.ReviewRow row = interactionRepository.findReviewById(reviewId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    Long userId = CurrentUserContext.optional().map(CurrentUser::userId).orElse(null);
    if (!REVIEW_STATUS_PUBLISHED.equals(row.status()) && (userId == null || !userId.equals(row.userId()))) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    return toUserReviewView(row, userId);
  }

  ReviewView reviewByOrder(long orderId) {
    long userId = requireUser().userId();
    return interactionRepository.findReviewByOrder(userId, orderId)
        .map(row -> toUserReviewView(row, userId))
        .orElse(null);
  }

  PageResponse<ReviewView> storeReviews(long storeId, int page, int pageSize) {
    long total = interactionRepository.countStoreReviews(storeId);
    List<ReviewView> list = interactionRepository.listStoreReviews(storeId, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toPublicReviewView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  ReviewView submitReview(long orderId, ReviewCreateRequest request) {
    long userId = requireUser().userId();
    if (request.rating() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分不能为空");
    }
    if (request.rating() < 1 || request.rating() > 5) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分必须在 1 到 5 之间");
    }
    InteractionRepository.OrderReviewRow order = interactionRepository.findOrderForReview(userId, orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!DisplayOrderStatus.USED.code().equals(order.displayStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单完成后才能评价");
    }
    return interactionRepository.findReviewByOrder(userId, orderId)
        .map(row -> toUserReviewView(row, userId))
        .orElseGet(() -> {
          String labels = joinList(request.labels());
          String imageUrls = joinList(request.imageUrls());
          Long reviewId = interactionRepository.insertReview(userId, order, request, labels, imageUrls);
          interactionRepository.markOrderReviewed(orderId);
          return interactionRepository.findReviewById(reviewId)
              .map(row -> toUserReviewView(row, userId))
              .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        });
  }

  @Transactional
  ReviewHelpfulView toggleHelpful(long reviewId) {
    long userId = requireUser().userId();
    InteractionRepository.ReviewRow row = interactionRepository.findReviewById(reviewId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!REVIEW_STATUS_PUBLISHED.equals(row.status())) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    boolean already = interactionRepository.isHelpful(reviewId, userId);
    if (already) {
      interactionRepository.removeHelpful(reviewId, userId);
    } else {
      interactionRepository.insertHelpful(reviewId, userId);
    }
    int count = interactionRepository.countHelpful(reviewId);
    interactionRepository.updateHelpfulCount(reviewId, count);
    return new ReviewHelpfulView(!already, count);
  }

  @Transactional
  ReviewReportView reportReview(long reviewId, ReviewReportRequest request) {
    CurrentUser current = requireUser();
    InteractionRepository.ReviewRow row = interactionRepository.findReviewById(reviewId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!REVIEW_STATUS_PUBLISHED.equals(row.status())) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    String reason = request.reason() == null ? null : request.reason().trim();
    if (reason == null || reason.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "举报原因不能为空");
    }
    var existing = interactionRepository.findActiveReport(reviewId, current.userId());
    if (existing.isPresent()) {
      return new ReviewReportView(existing.get().id(), existing.get().status());
    }
    Long reportId = interactionRepository.insertReport(
        reviewId, current.userId(), reason, request.detail(), joinList(request.evidenceUrls()));
    interactionRepository.refreshReportedCount(reviewId);
    interactionRepository.insertSysAuditLog(
        "user", current.accountId(), "review_report", "review", reviewId,
        "举报评价 " + reviewId + "：" + reason);
    return new ReviewReportView(reportId, "submitted");
  }

  // ============ 商家端 ============

  PageResponse<ReviewView> merchantReviews(String statusFilter, Boolean replied, int page, int pageSize) {
    long merchantId = currentMerchantId();
    String normalized = normalizeReviewStatus(statusFilter);
    long total = interactionRepository.countMerchantReviews(merchantId, normalized, replied);
    List<ReviewView> list = interactionRepository.listMerchantReviews(merchantId, normalized, replied, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toMerchantReviewView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  ReviewView merchantReviewDetail(long reviewId) {
    long merchantId = currentMerchantId();
    return interactionRepository.findReviewForMerchant(merchantId, reviewId)
        .map(this::toMerchantReviewView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  ReviewView merchantReply(long reviewId, MerchantReviewReplyRequest request) {
    long merchantId = currentMerchantId();
    InteractionRepository.ReviewRow row = interactionRepository.findReviewForMerchant(merchantId, reviewId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (interactionRepository.findReplyByReview(reviewId).isPresent()) {
      // 幂等：已回复直接返回原结果
      return interactionRepository.findReviewForMerchant(merchantId, reviewId)
          .map(this::toMerchantReviewView)
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    String content = request.content() == null ? "" : request.content().trim();
    if (content.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "回复内容不能为空");
    }
    interactionRepository.insertReply(reviewId, merchantId, content);
    interactionRepository.markReviewReplied(reviewId);
    interactionRepository.insertSysAuditLog(
        "merchant", requireMerchant().accountId(), "review_reply", "review", reviewId,
        "商家回复评价 " + reviewId);
    return interactionRepository.findReviewForMerchant(merchantId, reviewId)
        .map(this::toMerchantReviewView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  // ============ 后台端 ============

  PageResponse<ReviewView> adminReviews(String statusFilter, Boolean reported, int page, int pageSize) {
    requireAdmin();
    String normalized = normalizeReviewStatus(statusFilter);
    long total = interactionRepository.countAdminReviews(normalized, reported);
    List<ReviewView> list = interactionRepository.listAdminReviews(normalized, reported, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toAdminReviewView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  ReviewView adminAudit(long reviewId, AdminReviewAuditRequest request) {
    CurrentUser admin = requireAdmin();
    InteractionRepository.ReviewRow row = interactionRepository.findReviewById(reviewId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    String action = request.action() == null ? "" : request.action().trim().toLowerCase();
    String fromStatus = row.status();
    String toStatus = switch (action) {
      case "pass", "restore" -> REVIEW_STATUS_PUBLISHED;
      case "hide" -> REVIEW_STATUS_HIDDEN;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "审核动作不正确");
    };
    if (!fromStatus.equals(toStatus)) {
      interactionRepository.updateReviewStatus(reviewId, toStatus);
    }
    interactionRepository.insertReviewAuditLog(reviewId, action, fromStatus, toStatus, admin.accountId(), request.remark());
    if ("pass".equals(action) || "hide".equals(action)) {
      interactionRepository.markReportsHandled(reviewId, admin.accountId());
      interactionRepository.refreshReportedCount(reviewId);
    }
    interactionRepository.insertSysAuditLog(
        "admin", admin.accountId(), "review_audit_" + action, "review", reviewId,
        "评价 " + reviewId + " 审核：" + fromStatus + "->" + toStatus);
    return interactionRepository.findReviewById(reviewId)
        .map(this::toAdminReviewView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  // ============ 视图映射 ============

  private ReviewView toUserReviewView(InteractionRepository.ReviewRow row, Long currentUserId) {
    Boolean helpfulByMe = currentUserId == null ? null : interactionRepository.isHelpful(row.id(), currentUserId);
    return new ReviewView(
        row.id(), row.orderId(), row.orderNo(), row.orderTitle(),
        row.storeId(), row.storeName(),
        row.rating(), row.content(), splitList(row.labels()), splitList(row.imageUrls()),
        row.helpfulCount(), row.reportedCount(), helpfulByMe,
        row.status(), row.replied(), row.replyContent(), row.repliedAt(), row.createdAt(),
        null, null, null);
  }

  private ReviewView toMerchantReviewView(InteractionRepository.ReviewRow row) {
    return new ReviewView(
        row.id(), row.orderId(), row.orderNo(), row.orderTitle(),
        row.storeId(), row.storeName(),
        row.rating(), row.content(), splitList(row.labels()), splitList(row.imageUrls()),
        row.helpfulCount(), row.reportedCount(), null,
        row.status(), row.replied(), row.replyContent(), row.repliedAt(), row.createdAt(),
        maskNickname(row.userNickname()), null, null);
  }

  private ReviewView toAdminReviewView(InteractionRepository.ReviewRow row) {
    List<String> reportReasons = interactionRepository.listActiveReportReasons(row.id());
    List<String> reportEvidenceUrls = interactionRepository.listActiveReportEvidenceUrls(row.id()).stream()
        .flatMap(value -> splitList(value).stream())
        .toList();
    return new ReviewView(
        row.id(), row.orderId(), row.orderNo(), row.orderTitle(),
        row.storeId(), row.storeName(),
        row.rating(), row.content(), splitList(row.labels()), splitList(row.imageUrls()),
        row.helpfulCount(), row.reportedCount(), null,
        row.status(), row.replied(), row.replyContent(), row.repliedAt(), row.createdAt(),
        maskNickname(row.userNickname()), reportReasons.isEmpty() ? null : reportReasons,
        reportEvidenceUrls.isEmpty() ? null : reportEvidenceUrls);
  }

  private ReviewView toPublicReviewView(InteractionRepository.ReviewRow row) {
    return new ReviewView(
        row.id(), row.orderId(), row.orderNo(), row.orderTitle(),
        row.storeId(), row.storeName(),
        row.rating(), row.content(), splitList(row.labels()), splitList(row.imageUrls()),
        row.helpfulCount(), null, null,
        row.status(), row.replied(), row.replyContent(), row.repliedAt(), row.createdAt(),
        maskNickname(row.userNickname()), null, null);
  }

  // ============ 鉴权工具 ============

  private CurrentUser requireUser() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.USER) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return current;
  }

  private CurrentUser requireMerchant() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.MERCHANT) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return current;
  }

  private CurrentUser requireAdmin() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.ADMIN) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return current;
  }

  private long currentMerchantId() {
    long accountId = requireMerchant().accountId();
    Long merchantId = jdbcTemplate.query(
        "select id from merchant_profile where account_id = ? and is_deleted = 0 limit 1",
        rs -> rs.next() ? rs.getLong("id") : null,
        accountId);
    if (merchantId == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "商家资料不存在");
    }
    return merchantId;
  }

  // ============ 字符串工具 ============

  private String normalizeReviewStatus(String value) {
    if (value == null || value.isBlank()) return null;
    String v = value.trim().toLowerCase();
    return switch (v) {
      case "published", "hidden", "reported" -> v;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "状态不正确");
    };
  }

  private List<String> splitList(String value) {
    if (value == null || value.isBlank()) return List.of();
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  private String joinList(List<String> values) {
    if (values == null || values.isEmpty()) return null;
    return values.stream()
        .map(v -> v == null ? "" : v.trim())
        .filter(s -> !s.isEmpty())
        .distinct()
        .reduce((a, b) -> a + "," + b)
        .orElse(null);
  }

  private String maskNickname(String nickname) {
    if (nickname == null || nickname.isBlank()) return "匿名用户";
    if (nickname.length() <= 2) return nickname.charAt(0) + "*";
    return nickname.charAt(0) + "***" + nickname.charAt(nickname.length() - 1);
  }
}
