package com.aituan.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aituan.TestAuthSupport;
import com.aituan.common.api.PageResponse;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 客服会话归属校验：跨商家访问、用户访问他人会话、对已关闭会话发消息 均应拒绝。 */
@SpringBootTest
@ActiveProfiles("test")
class SupportServiceTest {

  @Autowired private SupportService supportService;

  @AfterEach
  void cleanup() {
    TestAuthSupport.clear();
  }

  @Test
  void merchantCannotAccessOtherMerchantSession() {
    // 会话 1 属于商家 1（账号 2）。商家 2（账号 21）查看应当 NOT_FOUND
    TestAuthSupport.loginAsMerchant(21L);
    assertThatThrownBy(() -> supportService.merchantSessionDetail(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test
  void merchantCannotSendMessageToOtherMerchantSession() {
    TestAuthSupport.loginAsMerchant(21L);
    assertThatThrownBy(() -> supportService.merchantSendMessage(
        1L, new SupportMessageCreateRequest("我是隔壁商家")))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test
  void merchantCannotSendMessageToClosedSession() {
    // 会话 3 已 closed，归属商家 3（账号 22）；正确归属下尝试发送也应被状态机拒绝
    TestAuthSupport.loginAsMerchant(22L);
    assertThatThrownBy(() -> supportService.merchantSendMessage(
        3L, new SupportMessageCreateRequest("再补一句")))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ORDER_STATE_INVALID));
  }

  @Test
  void userCanCreatePlatformSessionWithoutStoreOrTopic() {
    TestAuthSupport.loginAsUser(1L, 1L);
    SupportSessionView session = supportService.createUserSession(
        new SupportSessionCreateRequest(null, null, null));

    assertThat(session.storeId()).isEqualTo(0L);
    assertThat(session.storeName()).isEqualTo("平台客服");
    assertThat(session.topic()).isEqualTo("平台客服");
    assertThat(session.serviceScope()).isEqualTo("platform");
    assertThat(session.assistantMode()).isEqualTo("ai");
  }

  @Test
  void userMessageTriggersKeywordAutoReply() {
    TestAuthSupport.loginAsUser(1L, 1L);
    SupportMessageView sent = supportService.userSendMessage(
        1L, new SupportMessageCreateRequest("配送还要多久，能催一下吗"));
    SupportSessionDetailView detail = supportService.userSessionDetail(1L);

    assertThat(sent.content()).contains("配送");
    assertThat(detail.messages())
        .anySatisfy(message -> {
          assertThat(message.senderType()).isEqualTo("merchant");
          assertThat(message.messageKind()).isEqualTo("auto_reply");
          assertThat(message.content()).contains("催单");
        });
  }

  @Test
  void platformSessionCanBeTransferredToHumanByKeyword() {
    TestAuthSupport.loginAsUser(1L, 1L);
    SupportSessionView session = supportService.createUserSession(
        new SupportSessionCreateRequest(null, null, null));

    supportService.userSendMessage(session.id(), new SupportMessageCreateRequest("我要转人工"));
    SupportSessionDetailView detail = supportService.userSessionDetail(session.id());

    assertThat(detail.session().assistantMode()).isEqualTo("human");
    assertThat(detail.session().serviceScope()).isEqualTo("platform");
    assertThat(detail.messages())
        .anySatisfy(message -> {
          assertThat(message.senderType()).isEqualTo("platform");
          assertThat(message.messageKind()).isEqualTo("handoff");
          assertThat(message.content()).contains("人工客服");
        });
  }

  @Test
  void merchantCanRequestPlatformHumanIntervention() {
    TestAuthSupport.loginAsMerchant(2L);
    SupportSessionView escalated = supportService.requestPlatformIntervention(1L);

    assertThat(escalated.platformInterventionStatus()).isEqualTo("active");
    assertThat(escalated.assistantMode()).isEqualTo("human");
    assertThat(escalated.serviceScope()).isEqualTo("platform");

    SupportSessionDetailView detail = supportService.merchantSessionDetail(1L);
    assertThat(detail.messages())
        .anySatisfy(message -> {
          assertThat(message.senderType()).isEqualTo("platform");
          assertThat(message.messageKind()).isEqualTo("platform_intervention");
          assertThat(message.content()).contains("平台客服已介入");
        });
  }

  @Test
  void adminCanReplyTransferredPlatformSession() {
    TestAuthSupport.loginAsUser(1L, 1L);
    SupportSessionView session = supportService.createUserSession(
        new SupportSessionCreateRequest(null, null, null));
    supportService.userHandoffToHuman(session.id());

    TestAuthSupport.loginAsAdmin(3L);
    PageResponse<SupportSessionView> sessions = supportService.adminPlatformSessions("open", 1, 20);
    assertThat(sessions.list())
        .anySatisfy(row -> assertThat(row.id()).isEqualTo(session.id()));

    SupportMessageView reply = supportService.adminSendPlatformMessage(
        session.id(), new SupportMessageCreateRequest("平台人工已接入，请补充订单号"));
    assertThat(reply.senderType()).isEqualTo("platform");
    assertThat(reply.messageKind()).isEqualTo("text");
  }
}
