package com.aituan.message;

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

  PageResponse<MessageView> listMessages(int page, int pageSize, String type) {
    long userId = CurrentUserContext.required().userId();
    List<MessageView> list = messageRepository.listMessages(userId, type, (page - 1) * pageSize, pageSize).stream()
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
    return PageResponse.of(list, page, pageSize, messageRepository.countMessages(userId, type));
  }

  void markRead(long messageId) {
    long userId = CurrentUserContext.required().userId();
    messageRepository.markRead(userId, messageId);
  }

  void markAllRead() {
    long userId = CurrentUserContext.required().userId();
    messageRepository.markAllRead(userId);
  }
}
