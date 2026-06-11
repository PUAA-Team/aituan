package com.aituan.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aituan.TestAuthSupport;
import com.aituan.common.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AiAgentServiceTest {

  @Autowired private AiAgentService aiAgentService;

  @AfterEach
  void cleanup() {
    TestAuthSupport.clear();
  }

  @Test
  void assistantPersistsMessagesAndReturnsHistoryWithSteps() {
    TestAuthSupport.loginAsUser(1L, 1L);

    AiAssistantResponse response = aiAgentService.userAssistant(
        CurrentUserContext.required(),
        new AiAssistantMessageRequest("帮我看看最近订单和优惠券", null));

    assertThat(response.conversationId()).isNotBlank();
    assertThat(response.steps())
        .extracting(AiAssistantStep::title)
        .contains("调用了订单信息", "调用了优惠券信息");

    AiAssistantHistoryResponse history = aiAgentService.currentConversation(CurrentUserContext.required());

    assertThat(history.conversationId()).isEqualTo(response.conversationId());
    assertThat(history.messages()).hasSizeGreaterThanOrEqualTo(2);
    assertThat(history.messages())
        .anySatisfy(message -> {
          assertThat(message.role()).isEqualTo("user");
          assertThat(message.content()).contains("最近订单");
        })
        .anySatisfy(message -> {
          assertThat(message.role()).isEqualTo("assistant");
          assertThat(message.steps())
              .extracting(AiAssistantStep::title)
              .contains("调用了订单信息", "调用了优惠券信息");
        });
  }
}
