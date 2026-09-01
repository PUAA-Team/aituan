package com.aituan.engagementplatform.interaction;

import com.aituan.common.exception.BusinessException;
import com.aituan.engagementplatform.client.PlatformRemoteClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ReviewOrderMarkCompensation {
  private final InteractionRepository repository;
  private final PlatformRemoteClient remoteClient;
  private final int batchSize;
  private final int maxAttempts;
  private final int lockTimeoutSeconds;

  ReviewOrderMarkCompensation(
      InteractionRepository repository,
      PlatformRemoteClient remoteClient,
      @Value("${aituan.compensation.review-order-mark.batch-size:50}") int batchSize,
      @Value("${aituan.compensation.review-order-mark.max-attempts:10}") int maxAttempts,
      @Value("${aituan.compensation.review-order-mark.lock-timeout-seconds:300}") int lockTimeoutSeconds) {
    this.repository = repository;
    this.remoteClient = remoteClient;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.lockTimeoutSeconds = lockTimeoutSeconds;
  }

  @Scheduled(
      initialDelayString = "${aituan.compensation.review-order-mark.initial-delay-ms:30000}",
      fixedDelayString = "${aituan.compensation.review-order-mark.fixed-delay-ms:60000}")
  void retryPending() {
    for (InteractionRepository.PendingOrderMark pending
        : repository.claimPendingOrderMarks(maxAttempts, batchSize, lockTimeoutSeconds)) {
      try {
        // PlatformRemoteClient 始终以 reviewId 构造同一幂等键，重试不会重复修改订单。
        remoteClient.markOrderReviewed(pending.orderId(), pending.reviewId());
        repository.markOrderSyncSucceeded(pending.reviewId());
      } catch (BusinessException exception) {
        repository.markOrderSyncFailed(pending.reviewId(), exception.getMessage());
      }
    }
  }
}
