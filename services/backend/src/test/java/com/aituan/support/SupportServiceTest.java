package com.aituan.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aituan.TestAuthSupport;
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
}
