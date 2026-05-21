package com.aituan.interaction;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.DisplayOrderStatus;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUserContext;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InteractionService {
  private final InteractionRepository interactionRepository;

  InteractionService(InteractionRepository interactionRepository) {
    this.interactionRepository = interactionRepository;
  }

  PageResponse<ReviewView> reviews(int page, int pageSize) {
    long userId = CurrentUserContext.required().userId();
    long total = interactionRepository.countReviews(userId);
    List<ReviewView> list = interactionRepository.listReviews(userId, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toReviewView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  ReviewView review(long orderId) {
    long userId = CurrentUserContext.required().userId();
    return interactionRepository.findReviewByOrder(userId, orderId)
        .map(this::toReviewView)
        .orElse(null);
  }

  @Transactional
  ReviewView submitReview(long orderId, ReviewCreateRequest request) {
    long userId = CurrentUserContext.required().userId();
    InteractionRepository.OrderReviewRow order = interactionRepository.findOrderForReview(userId, orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!DisplayOrderStatus.USED.code().equals(order.displayStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单完成后才能评价");
    }
    return interactionRepository.findReviewByOrder(userId, orderId)
        .map(this::toReviewView)
        .orElseGet(() -> {
          String labels = joinLabels(request.labels());
          Long reviewId = interactionRepository.insertReview(userId, order, request, labels);
          interactionRepository.markOrderReviewed(orderId);
          return interactionRepository.findReviewById(userId, reviewId)
              .map(this::toReviewView)
              .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        });
  }

  private ReviewView toReviewView(InteractionRepository.ReviewRow row) {
    return new ReviewView(
        row.id(),
        row.orderId(),
        row.orderNo(),
        row.orderTitle(),
        row.storeId(),
        row.storeName(),
        row.rating(),
        row.content(),
        splitLabels(row.labels()),
        row.status(),
        row.replied(),
        row.replyContent(),
        row.repliedAt(),
        row.createdAt());
  }

  private List<String> splitLabels(String labels) {
    if (labels == null || labels.isBlank()) {
      return List.of();
    }
    return Arrays.stream(labels.split(","))
        .map(String::trim)
        .filter(label -> !label.isBlank())
        .toList();
  }

  private String joinLabels(List<String> labels) {
    if (labels == null || labels.isEmpty()) {
      return null;
    }
    return labels.stream()
        .map(label -> label == null ? "" : label.trim())
        .filter(label -> !label.isBlank())
        .distinct()
        .reduce((left, right) -> left + "," + right)
        .orElse(null);
  }
}
