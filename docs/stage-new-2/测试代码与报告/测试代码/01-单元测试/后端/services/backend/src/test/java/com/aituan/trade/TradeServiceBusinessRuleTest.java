package com.aituan.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aituan.TestAuthSupport;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 交易 Service 业务规则：金额计算、起送价、退款状态和券码核销幂等边界。 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TradeServiceBusinessRuleTest {

  @Autowired private TradeService tradeService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanup() {
    TestAuthSupport.clear();
  }

  @Test
  void previewShouldCalculateTakeawayAmountFeesAndMinimumOrder() {
    TestAuthSupport.loginAsUser(1L, 1L);
    long addressId = insertAddressWithLocation();

    CheckoutPreviewView preview = tradeService.preview(new CheckoutPreviewRequest(
        1L,
        "takeaway",
        addressId,
        List.of(new CheckoutItemRequest(1002L, 2)),
        "少放辣",
        "merchant_decide",
        null,
        null));

    assertThat(preview.amount()).isEqualByComparingTo(new BigDecimal("39.80"));
    assertThat(preview.deliveryFee()).isEqualByComparingTo(new BigDecimal("4.00"));
    assertThat(preview.packageFee()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(preview.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(preview.payableAmount()).isEqualByComparingTo(new BigDecimal("43.80"));
    assertThat(preview.startPrice()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(preview.startPriceMissing()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(preview.minimumOrderMet()).isTrue();
    assertThat(preview.deliverable()).isTrue();
    assertThat(preview.items()).singleElement().satisfies(item -> {
      assertThat(item.itemId()).isEqualTo(1002L);
      assertThat(item.quantity()).isEqualTo(2);
      assertThat(item.totalPrice()).isEqualByComparingTo(new BigDecimal("39.80"));
    });
  }

  @Test
  void createOrderShouldRejectWhenItemAmountBelowStartPrice() {
    TestAuthSupport.loginAsUser(1L, 1L);
    long addressId = insertAddressWithLocation();

    CreateOrderRequest request = new CreateOrderRequest(
        1L,
        "takeaway",
        addressId,
        List.of(new CheckoutItemRequest(1002L, 1)),
        "单件未达起送价",
        "merchant_decide",
        null,
        null,
        "service-test-below-start-price");

    assertThatThrownBy(() -> tradeService.createOrder(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("起送")
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));
  }

  @Test
  void refundShouldPersistRefundStatusReasonAndDeliveryTask() {
    TestAuthSupport.loginAsUser(1L, 1L);

    OrderDetailView detail = tradeService.refundOrderForUser(9011L, new RefundRequest("用户主动取消"));

    assertThat(detail.displayStatus()).isEqualTo("refunded");
    assertThat(detail.paymentStatus()).isEqualTo("refunded");
    assertThat(detail.fulfillmentStatus()).isEqualTo("refunded");
    assertThat(detail.refundStatus()).isEqualTo("succeeded");
    assertThat(detail.refundAmount()).isEqualByComparingTo(new BigDecimal("37.80"));
    assertThat(detail.refundReason()).isEqualTo("用户主动取消");
    assertThat(detail.refundedAt()).isNotNull();
    assertThat(detail.refundableByUser()).isFalse();
    assertThat(deliveryTaskStage(9011L)).isEqualTo("refunded");
  }

  @Test
  void redeemVoucherShouldRejectDuplicateRedeem() {
    TestAuthSupport.loginAsAdmin(3L);

    OrderDetailView first = tradeService.redeemVoucher("88001234");
    assertThat(first.displayStatus()).isEqualTo("used");
    assertThat(first.fulfillmentStatus()).isEqualTo("voucher_used");
    assertThat(voucherStatus("88001234")).isEqualTo("used");

    assertThatThrownBy(() -> tradeService.redeemVoucher("88001234"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已核销")
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ORDER_STATE_INVALID));
  }

  private long insertAddressWithLocation() {
    jdbcTemplate.update(
        """
        insert into user_address(user_id, contact_name, contact_phone, province, city, district,
                                 detail_address, longitude, latitude, tag_name, is_default, delivery_note)
        values (1, '李同学', '18800002222', '北京市', '北京市', '海淀区',
                'Service 单测定位地址', 116.313600, 39.982300, '测试', 0, 'Service 单测')
        """);
    return jdbcTemplate.queryForObject(
        "select id from user_address where detail_address = ? order by id desc limit 1",
        Long.class,
        "Service 单测定位地址");
  }

  private String deliveryTaskStage(long orderId) {
    return jdbcTemplate.queryForObject(
        "select current_stage from delivery_task where order_id = ? and is_deleted = 0",
        String.class,
        orderId);
  }

  private String voucherStatus(String voucherCode) {
    return jdbcTemplate.queryForObject(
        "select status from order_voucher where voucher_code = ? and is_deleted = 0 order by id limit 1",
        String.class,
        voucherCode);
  }
}
