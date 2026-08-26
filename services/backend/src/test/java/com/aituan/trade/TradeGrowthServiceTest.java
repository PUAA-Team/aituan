package com.aituan.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.aituan.TestAuthSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 成长值规则：支付不加，订单完成后加，退款扣回。 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TradeGrowthServiceTest {

  @Autowired private TradeService tradeService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanup() {
    TestAuthSupport.clear();
  }

  @Test
  void payShouldNotAddGrowth() {
    TestAuthSupport.loginAsUser(1L, 1L);

    int before = growthValue();
    tradeService.pay(1L, new PayOrderRequest("mock"));

    assertThat(growthValue()).isEqualTo(before);
    assertThat(growthLogCount(1L, "order_complete")).isZero();
  }

  @Test
  void redeemVoucherShouldAddGrowthAndRefundShouldReturnIt() {
    TestAuthSupport.loginAsAdmin(3L);

    int before = growthValue();
    tradeService.redeemVoucher("88001234");

    assertThat(growthValue()).isEqualTo(before + 168);
    assertThat(growthLogCount(3L, "order_complete")).isEqualTo(1);

    tradeService.refundOrderForStaff(3L, new RefundRequest("测试退款"));
    assertThat(growthValue()).isEqualTo(before);
    assertThat(growthLogCount(3L, "order_refund")).isEqualTo(1);

    tradeService.refundOrderForStaff(3L, new RefundRequest("重复退款"));
    assertThat(growthValue()).isEqualTo(before);
    assertThat(growthLogCount(3L, "order_refund")).isEqualTo(1);
  }

  @Test
  void takeawayCompletionShouldAddGrowthOnce() {
    TestAuthSupport.loginAsMerchant(29L);

    int before = growthValue();
    tradeService.completeTakeawayOrder(9016L, new TakeawayOrderActionRequest("测试完成"));

    assertThat(growthValue()).isEqualTo(before + 33);
    assertThat(growthLogCount(9016L, "order_complete")).isEqualTo(1);
  }

  private int growthValue() {
    Integer value = jdbcTemplate.queryForObject(
        "select growth_value from user_profile where id = 1",
        Integer.class);
    return value == null ? 0 : value;
  }

  private int growthLogCount(long orderId, String sourceType) {
    Integer value = jdbcTemplate.queryForObject(
        "select count(1) from member_growth_log where order_id = ? and source_type = ?",
        Integer.class,
        orderId,
        sourceType);
    return value == null ? 0 : value;
  }
}
