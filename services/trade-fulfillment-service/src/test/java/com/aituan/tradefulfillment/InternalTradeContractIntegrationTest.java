package com.aituan.tradefulfillment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InternalTradeContractIntegrationTest {
  private static final long ORDER_ID = 990001L;

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void prepareOrderSnapshot() {
    jdbcTemplate.update(
        "insert into order_main(id, order_no, user_id, store_id, merchant_id, coupon_id, store_name, order_type, title, display_status, payment_status, fulfillment_status, amount, delivery_fee, package_fee, discount_amount, payable_amount, refund_status) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ORDER_ID, "TEST-INTERNAL-990001", 7001L, 8001L, 9001L, 6001L, "契约测试门店", "takeaway", "契约测试订单",
        "used", "paid", "completed", 25, 2, 1, 3, 25, "none");
    jdbcTemplate.update(
        "insert into order_item(order_id, item_id, sku_id, item_name, business_type, category_id, quantity, unit_price, total_price) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ORDER_ID, 5001L, 5101L, "契约测试商品", "takeaway", 4001L, 1, 25, 25);
  }

  @Test
  void exposesAllInternalContractsWithSnapshotFields() throws Exception {
    mockMvc.perform(internal(get("/internal/orders/{orderId}/snapshot", ORDER_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.orderId").value(ORDER_ID))
        .andExpect(jsonPath("$.data.merchantId").value(9001))
        .andExpect(jsonPath("$.data.paymentStatus").value("paid"));

    mockMvc.perform(internal(get("/internal/orders/{orderId}/review-eligibility", ORDER_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.eligible").value(true))
        .andExpect(jsonPath("$.data.order.userId").value(7001));

    mockMvc.perform(internal(post("/internal/orders/{orderId}/reviewed", ORDER_ID))
            .contentType(MediaType.APPLICATION_JSON).content("{\"reviewId\":3001}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("reviewed"));

    mockMvc.perform(internal(get("/internal/users/{userId}/purchase-signals", 7001)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.orderCount").value(1))
        .andExpect(jsonPath("$.data.categoryIds[0]").value(4001));

    mockMvc.perform(internal(get("/internal/metrics/stores/{storeId}/orders", 8001)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.orderCount").value(1))
        .andExpect(jsonPath("$.data.amount").value(25));

    mockMvc.perform(internal(get("/internal/metrics/platform/orders")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.paidOrderCount").value(1));
  }

  @Test
  void rejectsMissingHeadersAndUnknownCaller() throws Exception {
    mockMvc.perform(get("/internal/orders/{orderId}/snapshot", ORDER_ID))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/internal/orders/{orderId}/snapshot", ORDER_ID)
            .header("X-Request-Id", "req-internal-contract")
            .header("X-Caller-Service", "api-gateway")
            .header("X-Service-Token", "test-internal-token"))
        .andExpect(status().isForbidden());
  }

  private MockHttpServletRequestBuilder internal(MockHttpServletRequestBuilder request) {
    return request
        .header("X-Request-Id", "req-internal-contract")
        .header("X-Caller-Service", "engagement-platform-service")
        .header("X-Service-Token", "test-internal-token");
  }
}
