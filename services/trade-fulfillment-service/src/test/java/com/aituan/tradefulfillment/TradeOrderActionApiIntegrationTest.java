package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.exception.ErrorCode;
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
class TradeOrderActionApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void userCanCancelUnpaidTakeawayOrder() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createOrder(userToken, 1, "takeaway", 1002, "cancel-unpaid");

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/cancel", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"remark":"临时有事"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.displayStatus").value("cancelled"))
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("cancelled"));
  }

  @Test
  void userCanCancelPaidTakeawayBeforeMerchantAccepts() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createPaidTakeawayOrder(userToken, "cancel-paid-before-accept");

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/cancel", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"remark":"还没接单时取消"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.displayStatus").value("refunded"))
        .andExpect(jsonPath("$.data.paymentStatus").value("refunded"))
        .andExpect(jsonPath("$.data.refundStatus").value("succeeded"));
  }

  @Test
  void rejectsCancelAndAddressChangeAfterMerchantAccepts() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);
    long orderId = createPaidTakeawayOrder(userToken, "reject-cancel-after-accept");
    acceptOrder(merchantToken, orderId);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/cancel", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.ORDER_STATE_INVALID));

    mockMvc.perform(ApiTestSupport.bearer(put("/api/app/trade/orders/{orderId}/delivery-address", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"addressId":2}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.ORDER_STATE_INVALID));
  }

  @Test
  void userCanRemindPaidTakeawayOrderWithoutMovingStatus() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createPaidTakeawayOrder(userToken, "remind-paid-takeaway");

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/remind", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"remark":"麻烦快一点"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.displayStatus").value("pending"))
        .andExpect(jsonPath("$.data.paymentStatus").value("paid"))
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("merchant_pending"));
  }

  @Test
  void rejectsRemindForVoucherOrder() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createOrder(userToken, 2, "group_buy", 2001, "remind-voucher-order");
    pay(userToken, orderId);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/remind", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.BUSINESS_RULE_VIOLATION));
  }

  @Test
  void userCanChangeDeliveryAddressBeforeMerchantAccepts() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createPaidTakeawayOrder(userToken, "change-address-before-accept");

    mockMvc.perform(ApiTestSupport.bearer(put("/api/app/trade/orders/{orderId}/delivery-address", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"addressId":2}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.addressSnapshot", not(blankOrNullString())));
  }

  @Test
  void rejectsMissingAddressId() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createPaidTakeawayOrder(userToken, "missing-address-id");

    mockMvc.perform(ApiTestSupport.bearer(put("/api/app/trade/orders/{orderId}/delivery-address", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(ApiTestSupport.badRequest());
  }

  private long createPaidTakeawayOrder(String userToken, String idempotencyKey) throws Exception {
    long orderId = createOrder(userToken, 1, "takeaway", 1002, idempotencyKey);
    pay(userToken, orderId);
    return orderId;
  }

  private long createOrder(String userToken, long storeId, String businessType, long itemId, String idempotencyKey) throws Exception {
    int quantity = "takeaway".equals(businessType) ? 2 : 1;
    MvcResult result = mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":%d,"businessType":"%s","addressId":1,"items":[{"itemId":%d,"quantity":%d}],"idempotencyKey":"%s"}
                """.formatted(storeId, businessType, itemId, quantity, idempotencyKey)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id", greaterThan(0)))
        .andReturn();
    return dataId(result);
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

  private void acceptOrder(String merchantToken, long orderId) throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/accept", orderId), merchantToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
  }

  private long dataId(MvcResult result) throws Exception {
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    return root.path("data").path("id").asLong();
  }
}
