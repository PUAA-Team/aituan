package com.aituan.complaint;

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

/** 投诉状态机非法跃迁测试：跳过 accept 直接 resolve、对 resolved 再 resolve、对 closed 操作 均应拒绝。 */
@SpringBootTest
@ActiveProfiles("test")
class ComplaintServiceTest {

  @Autowired private ComplaintService complaintService;

  @AfterEach
  void cleanup() {
    TestAuthSupport.clear();
  }

  @Test
  void resolveOnPendingShouldBeRejected() {
    // 工单 1 当前状态 = pending，直接 resolve 应拒绝（要求 from=processing）
    TestAuthSupport.loginAsAdmin(3L);
    assertThatThrownBy(() -> complaintService.resolve(1L, new ComplaintActionRequest("强制完成")))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ORDER_STATE_INVALID));
  }

  @Test
  void resolveOnResolvedShouldBeRejected() {
    // 工单 3 已是 resolved，再 resolve 应拒绝
    TestAuthSupport.loginAsAdmin(3L);
    assertThatThrownBy(() -> complaintService.resolve(3L, new ComplaintActionRequest("重复处理")))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ORDER_STATE_INVALID));
  }

  @Test
  void anyActionOnClosedShouldBeRejected() {
    // 先把工单 3（resolved）关闭，再尝试 accept，必须拒绝
    TestAuthSupport.loginAsAdmin(3L);
    complaintService.close(3L, new ComplaintActionRequest("用户确认满意"));
    assertThatThrownBy(() -> complaintService.accept(3L, new ComplaintActionRequest("重新受理")))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ORDER_STATE_INVALID));
  }
}
