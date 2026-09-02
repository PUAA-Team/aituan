package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TradeOpsApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void merchantCanOperateTakeawayOrderLifecycle() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);
    long orderId = createPaidTakeawayOrder(userToken, "ops-takeaway-order");

    mockMvc.perform(ApiTestSupport.bearer(get("/api/merchant/trade/orders"), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list[0].id").value(orderId))
        .andExpect(jsonPath("$.data.total", greaterThan(0)));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/accept", orderId), merchantToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"remark":"开始接单"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("accepted"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/prepare", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("preparing"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/ready", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("ready_for_delivery"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/delivery/advance", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("delivering"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/delivery/advance", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("delivered"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/complete", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.displayStatus").value("used"))
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("completed"));
  }

  @Test
  void adminCanRedeemVoucherAndConfirmBooking() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String adminToken = ApiTestSupport.adminToken(jwtTokenService);
    long orderId = createPaidGroupBuyOrder(userToken, "ops-voucher-booking");

    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/trade/vouchers"), adminToken)
            .param("status", "unused"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list", hasSize(greaterThan(0))));

    String voucherCode = voucherCode(orderId);
    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/trade/vouchers/{voucherCode}", voucherCode), adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.voucherCode").value(voucherCode));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/trade/vouchers/{voucherCode}/redeem", voucherCode), adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.voucher.status").value("used"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/trade/vouchers/{voucherCode}/redeem", voucherCode), adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code", is(5002)));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/booking", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"contactName":"李同学","contactPhone":"18800001111","bookingDate":"2026-09-01","bookingTimeSlot":"18:00-20:00","guestCount":2}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());

    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/trade/bookings"), adminToken)
            .param("status", "pending")
            .param("businessType", "group_buy"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list[0].booking.orderId").value(orderId));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/trade/orders/{orderId}/booking/confirm", orderId), adminToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"remark":"已确认预约"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.storeConfirmStatus").value("confirmed"))
        .andExpect(jsonPath("$.data.storeConfirmRemark").value("已确认预约"));
  }

  private long createPaidTakeawayOrder(String userToken, String idempotencyKey) throws Exception {
    long orderId = createOrder(userToken, 1, "takeaway", 1002, 2, idempotencyKey);
    pay(userToken, orderId);
    return orderId;
  }

  private long createPaidGroupBuyOrder(String userToken, String idempotencyKey) throws Exception {
    long orderId = createOrder(userToken, 2, "group_buy", 2001, 1, idempotencyKey);
    pay(userToken, orderId);
    return orderId;
  }

  private long createOrder(String userToken, long storeId, String businessType, long itemId, int quantity, String idempotencyKey) throws Exception {
    MvcResult result = mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":%d,"businessType":"%s","addressId":1,"items":[{"itemId":%d,"quantity":%d}],"idempotencyKey":"%s"}
                """.formatted(storeId, businessType, itemId, quantity, idempotencyKey)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andReturn();
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    return root.path("data").path("id").asLong();
  }

  private void pay(String userToken, long orderId) throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/pay", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"paymentMode":"mock"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
  }

  private String voucherCode(long orderId) {
    return String.format("88%08d", Math.floorMod(orderId, 100_000_000));
  }
}
