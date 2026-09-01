package com.aituan.engagementplatform.client;

import com.aituan.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StationMessageClient {
  private static final Logger log = LoggerFactory.getLogger(StationMessageClient.class);
  private final PlatformRemoteClient remoteClient;
  private final NotificationFailureRecorder failureRecorder;

  StationMessageClient(
      PlatformRemoteClient remoteClient,
      NotificationFailureRecorder failureRecorder) {
    this.remoteClient = remoteClient;
    this.failureRecorder = failureRecorder;
  }

  public void review(long userId, String title, String content, Long orderId, long reviewId) {
    publish(userId, "review", title, content, "评价", orderId, "review", reviewId);
  }

  public void complaint(long userId, String title, String content, Long orderId, long complaintId, String badgeText) {
    publish(userId, "complaint", title, content, badgeText, orderId, "complaint", complaintId);
  }

  public void support(long userId, String title, String content, Long orderId, long sessionId, String badgeText) {
    publish(userId, "support", title, content, badgeText, orderId, "support_session", sessionId);
  }

  private void publish(
      long userId, String type, String title, String content, String badgeText,
      Long orderId, String targetType, Long targetId) {
    PlatformRemoteClient.MessageCommand command = new PlatformRemoteClient.MessageCommand(
        userId, type, title, content, badgeText, orderId, targetType, targetId);
    String idempotencyKey = type + "-" + targetId + "-"
        + UUID.nameUUIDFromBytes(title.getBytes(StandardCharsets.UTF_8));
    try {
      remoteClient.publishMessage(command, idempotencyKey);
    } catch (BusinessException exception) {
      // 通知是非关键副作用。依赖不可用时保留本地业务结果，避免回滚评价、投诉或客服状态。
      try {
        failureRecorder.record(targetType, targetId, idempotencyKey, exception.getMessage());
      } catch (RuntimeException traceException) {
        // 追踪写入也不能反向破坏已经完成的主业务事务。
        log.error("Failed to record degraded station message, type={}, targetId={}",
            type, targetId, traceException);
      }
      log.warn("Station message degraded, type={}, targetId={}, idempotencyKey={}",
          type, targetId, idempotencyKey, exception);
    }
  }
}
