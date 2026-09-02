package com.aituan.identity.message;

import com.aituan.common.api.PageResponse;
import com.aituan.common.security.CurrentUserContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class MessageService {
  private final MessageRepository messageRepository;

  MessageService(MessageRepository messageRepository) {
    this.messageRepository = messageRepository;
  }

  PageResponse<MessageView> listMessages(String type, int page, int pageSize) {
    long userId = CurrentUserContext.required().userId();
    String normalizedType = type == null || type.isBlank() ? null : type.trim();
    List<MessageView> list = messageRepository.listMessages(userId, normalizedType, (page - 1) * pageSize, pageSize).stream()
        .map(row -> new MessageView(
            row.id(),
            row.type(),
            row.title(),
            row.content(),
            row.badgeText(),
            "unread".equals(row.readStatus()),
            row.relatedOrderId(),
            row.relatedTargetType(),
            row.relatedTargetId(),
            row.createdAt()))
        .toList();
    return PageResponse.of(list, page, pageSize, messageRepository.countMessages(userId, normalizedType));
  }

  void markRead(long messageId) {
    long userId = CurrentUserContext.required().userId();
    messageRepository.markRead(userId, messageId);
  }

  void markAllRead() {
    long userId = CurrentUserContext.required().userId();
    messageRepository.markAllRead(userId);
  }

  void markBatchRead(MessageBatchRequest request) {
    messageRepository.markRead(CurrentUserContext.required().userId(), normalizedIds(request));
  }

  void markBatchUnread(MessageBatchRequest request) {
    messageRepository.markUnread(CurrentUserContext.required().userId(), normalizedIds(request));
  }

  void deleteBatch(MessageBatchRequest request) {
    messageRepository.softDelete(CurrentUserContext.required().userId(), normalizedIds(request));
  }

  private List<Long> normalizedIds(MessageBatchRequest request) {
    return request.messageIds().stream().distinct().toList();
  }
}
