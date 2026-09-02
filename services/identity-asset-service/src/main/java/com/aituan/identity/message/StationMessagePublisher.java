package com.aituan.identity.message;

import org.springframework.stereotype.Service;

@Service
public class StationMessagePublisher {
  private final MessageRepository messageRepository;

  public StationMessagePublisher(MessageRepository messageRepository) {
    this.messageRepository = messageRepository;
  }

  public void order(long userId, String title, String content, String badgeText, Long orderId) {
    publish(userId, "order", title, content, badgeText, orderId, "order", orderId);
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

  public void publish(
      long userId,
      String type,
      String title,
      String content,
      String badgeText,
      Long relatedOrderId,
      String relatedTargetType,
      Long relatedTargetId) {
    messageRepository.insertStationMessage(
        userId,
        type,
        title,
        content,
        badgeText,
        relatedOrderId,
        relatedTargetType,
        relatedTargetId);
  }
}
