package com.aituan.tradefulfillment.trade.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.tradefulfillment.trade.repository.TradeRepository;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalTradeServiceUnitTest {
  @Mock private TradeRepository tradeRepository;

  private InternalTradeService service;

  @BeforeEach
  void setUp() {
    service = new InternalTradeService(tradeRepository);
  }

  @Test
  void completedPaidOrderShouldBeEligibleForReview() {
    when(tradeRepository.findOrderById(11L))
        .thenReturn(Optional.of(order(null, "used", "paid", "none")));

    ReviewEligibilityView result = service.reviewEligibility(11L);

    assertThat(result.eligible()).isTrue();
    assertThat(result.reason()).isNull();
    assertThat(result.order().orderId()).isEqualTo(11L);
  }

  @Test
  void unpaidOrderShouldNotBeEligibleForReview() {
    when(tradeRepository.findOrderById(11L))
        .thenReturn(Optional.of(order(null, "used", "unpaid", "none")));

    ReviewEligibilityView result = service.reviewEligibility(11L);

    assertThat(result.eligible()).isFalse();
    assertThat(result.reason()).isEqualTo("订单尚未支付");
  }

  @Test
  void refundedOrderShouldNotBeEligibleForReview() {
    when(tradeRepository.findOrderById(11L))
        .thenReturn(Optional.of(order(null, "used", "paid", "refunded")));

    ReviewEligibilityView result = service.reviewEligibility(11L);

    assertThat(result.eligible()).isFalse();
    assertThat(result.reason()).isEqualTo("退款订单不能评价");
  }

  @Test
  void reviewedOrderShouldNotBeEligibleForReview() {
    when(tradeRepository.findOrderById(11L))
        .thenReturn(Optional.of(order(91L, "used", "paid", "none")));

    ReviewEligibilityView result = service.reviewEligibility(11L);

    assertThat(result.eligible()).isFalse();
    assertThat(result.reason()).isEqualTo("订单已经评价");
  }

  @Test
  void markReviewedShouldRejectAConcurrentDifferentReview() {
    when(tradeRepository.findOrderById(11L))
        .thenReturn(Optional.of(order(null, "used", "paid", "none")));
    when(tradeRepository.markOrderReviewed(11L, 91L)).thenReturn(false);

    assertThatThrownBy(() -> service.markReviewed(11L, 91L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_STATE_INVALID)
        .hasMessage("订单已关联其他评价");
    verify(tradeRepository).markOrderReviewed(11L, 91L);
  }

  @Test
  void missingOrderShouldFailWithNotFound() {
    when(tradeRepository.findOrderById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.reviewEligibility(404L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
  }

  private OrderRow order(Long reviewId, String displayStatus, String paymentStatus, String refundStatus) {
    LocalDateTime now = LocalDateTime.of(2026, 9, 3, 12, 0);
    return new OrderRow(
        11L, "ORDER-11", 21L, 31L, 41L, null, reviewId,
        "测试门店", "service", "测试订单", displayStatus, paymentStatus, "completed", "mock",
        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
        "测试地址", BigDecimal.ONE, null, null, "无需餐具", 0, null,
        refundStatus, BigDecimal.ZERO, null, null, null, null, now, now, now, now);
  }
}
