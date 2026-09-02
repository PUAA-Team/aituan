package com.aituan.engagementplatform.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.aituan.common.enums.AccountType;
import com.aituan.common.security.CurrentUser;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SupportRepositoryTest {
  @Autowired SupportRepository repository;
  @Autowired SupportService service;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listMessagesReturnsMessagesForTheRequestedSession() {
    Long sessionId = repository.insertSession(
        "SS-REPO-" + UUID.randomUUID().toString().substring(0, 12),
        5001L,
        0L,
        0L,
        "客服消息查询回归",
        null,
        null,
        "平台客服",
        "测试用户",
        "platform",
        "ai");
    Long messageId = repository.insertMessage(sessionId, "user", 5001L, "查询优惠券规则", "text");

    assertThat(repository.listMessages(sessionId, 0, 50))
        .extracting(SupportRepository.MessageRow::id)
        .containsExactly(messageId);
  }

  @Test
  void userCanSendMessageWhenPersistedUserIdIsOutsideLongCache() {
    Long sessionId = repository.insertSession(
        "SS-REPO-" + UUID.randomUUID().toString().substring(0, 12),
        5001L,
        0L,
        0L,
        "客服用户标识回归",
        null,
        null,
        "平台客服",
        "测试用户",
        "platform",
        "human");
    CurrentUser currentUser = new CurrentUser(1L, Long.valueOf(5001L), AccountType.USER, "测试用户");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(currentUser, null, java.util.List.of()));

    SupportMessageView message = service.userSendMessage(
        sessionId,
        new SupportMessageCreateRequest("需要人工客服"));

    assertThat(message.content()).isEqualTo("需要人工客服");
  }
}
