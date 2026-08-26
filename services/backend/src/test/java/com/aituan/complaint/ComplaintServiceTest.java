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

  @Test
  void closeBeforeResolvedShouldBeRejected() {
    TestAuthSupport.loginAsAdmin(3L);

    assertThatThrownBy(() -> complaintService.close(1L, new ComplaintActionRequest("未受理直接关闭")))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ORDER_STATE_INVALID));
    assertThatThrownBy(() -> complaintService.close(2L, new ComplaintActionRequest("处理中直接关闭")))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ORDER_STATE_INVALID));
  }

  @Test
  void userCanSupplementOpenComplaint() {
    TestAuthSupport.loginAsUser(1L, 1L);
    ComplaintDetailView detail = complaintService.supplement(
        1L, new ComplaintSupplementRequest("骑手仍未联系我，请继续跟进"));

    assertThat(detail.logs())
        .anySatisfy(log -> {
          assertThat(log.action()).isEqualTo("supplement");
          assertThat(log.remark()).contains("继续跟进");
        });
  }

  @Test
  void adminTicketsCanFilterByOrderNoAndStoreNameFromSupportEntry() {
    TestAuthSupport.loginAsAdmin(3L);

    var byOrderNo = complaintService.adminTickets(
        null, null, "AT202605179011", null, 1, 20);
    assertThat(byOrderNo.list())
        .singleElement()
        .satisfies(ticket -> {
          assertThat(ticket.ticketNo()).isEqualTo("CP202605170001");
          assertThat(ticket.orderNo()).isEqualTo("AT202605179011");
        });

    var byStoreName = complaintService.adminTickets(
        null, null, null, "光影剧场", 1, 20);
    assertThat(byStoreName.list())
        .singleElement()
        .satisfies(ticket -> {
          assertThat(ticket.ticketNo()).isEqualTo("CP202605170002");
          assertThat(ticket.storeName()).isEqualTo("光影剧场");
        });
  }

  @Test
  void merchantCanListOnlyOwnComplaintTickets() {
    TestAuthSupport.loginAsUser(1L, 1L);
    ComplaintView submitted = complaintService.submit(new ComplaintCreateRequest(
        9011L,
        "service",
        "商家端投诉列表测试",
        "用户反馈希望商家能在自己的工单页看到",
        java.util.List.of()));

    TestAuthSupport.loginAsMerchant(2L);
    var page = complaintService.merchantTickets(null, null, null, 1, 20);

    assertThat(page.list()).anySatisfy(ticket -> {
      assertThat(ticket.id()).isEqualTo(submitted.id());
      assertThat(ticket.storeName()).isEqualTo("塔斯汀中国汉堡");
    });
    assertThat(page.list()).allSatisfy(ticket -> assertThat(ticket.merchantId()).isEqualTo(1L));
  }

  @Test
  void merchantCannotOpenOtherMerchantComplaintTicket() {
    TestAuthSupport.loginAsMerchant(30L);

    assertThatThrownBy(() -> complaintService.merchantTicketDetail(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.NOT_FOUND));
  }
}
