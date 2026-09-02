package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class TradeOpsNegativeApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void rejectsInvalidMerchantTakeawayStateTransitions() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);
    long unpaidOrderId = createOrder(userToken, 1, "takeaway", 1002, "ops-unpaid-accept");

    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/accept", unpaidOrderId), merchantToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.ORDER_STATE_INVALID));

    long paidOrderId = createOrder(userToken, 1, "takeaway", 1002, "ops-prepare-before-accept");
    pay(userToken, paidOrderId);
    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/prepare", paidOrderId), merchantToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.ORDER_STATE_INVALID));
  }

  @Test
  void rejectsRepeatedCompletionAndVoucherRedeem() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);
    String adminToken = ApiTestSupport.adminToken(jwtTokenService);
    long takeawayOrderId = createOrder(userToken, 1, "takeaway", 1002, "ops-repeat-complete");
    pay(userToken, takeawayOrderId);
    completeTakeawayOrder(merchantToken, takeawayOrderId);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/complete", takeawayOrderId), merchantToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.ORDER_STATE_INVALID));

    long voucherOrderId = createOrder(userToken, 2, "group_buy", 2001, "ops-repeat-redeem");
    pay(userToken, voucherOrderId);
    String voucherCode = String.format("88%08d", Math.floorMod(voucherOrderId, 100_000_000));
    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/trade/vouchers/{voucherCode}/redeem", voucherCode), adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/trade/vouchers/{voucherCode}/redeem", voucherCode), adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.ORDER_STATE_INVALID));
  }

  @Test
  void validatesOpsRequestParametersAndMissingResources() throws Exception {
    String adminToken = ApiTestSupport.adminToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/trade/bookings").param("pageSize", "0"), adminToken))
        .andExpect(ApiTestSupport.badRequest());

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/trade/vouchers/{voucherCode}/redeem", "NO-SUCH-CODE"), adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.NOT_FOUND));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/trade/orders/{orderId}/booking/confirm", 99999999L), adminToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.NOT_FOUND));
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

  private void completeTakeawayOrder(String merchantToken, long orderId) throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/accept", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/prepare", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/ready", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/delivery/advance", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/delivery/advance", orderId), merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
    mockMvc.perform(ApiTestSupport.bearer(post("/api/merchant/trade/orders/{orderId}/complete", orderId), merchantToken)
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
