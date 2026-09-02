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
class TradeLifecycleApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void userCanCreatePayListAndRefundTakeawayOrder() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String orderBody = """
        {
          "storeId":1,
          "businessType":"takeaway",
          "addressId":1,
          "items":[{"itemId":1002,"quantity":2}],
          "remark":"少放辣",
          "tablewareOption":"merchant_decide",
          "idempotencyKey":"trade-service-takeaway-lifecycle"
        }
        """;

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/checkout/preview"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content(orderBody))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.payableAmount", greaterThan(0.0)))
        .andExpect(jsonPath("$.data.minimumOrderMet").value(true));

    MvcResult created = mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content(orderBody))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id", greaterThan(0)))
        .andExpect(jsonPath("$.data.paymentStatus").value("unpaid"))
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.items[0].itemId").value(1002))
        .andReturn();

    long orderId = dataId(created);
    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content(orderBody))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/pay", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"paymentMode":"mock"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.displayStatus").value("pending"))
        .andExpect(jsonPath("$.data.paymentStatus").value("paid"))
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("merchant_pending"))
        .andExpect(jsonPath("$.data.deliveryTimeline.currentStage").value("merchant_pending"));

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/orders"), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list[0].id").value(orderId))
        .andExpect(jsonPath("$.data.total", greaterThan(0)));

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/orders/{orderId}/delivery/timeline", orderId), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.orderNo").isString())
        .andExpect(jsonPath("$.data.nodes", hasSize(7)));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/refund", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"reason":"用户取消测试订单"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.displayStatus").value("refunded"))
        .andExpect(jsonPath("$.data.paymentStatus").value("refunded"))
        .andExpect(jsonPath("$.data.refundStatus").value("succeeded"))
        .andExpect(jsonPath("$.data.refundReason").value("用户取消测试订单"));
  }

  @Test
  void rejectsPaymentModeAndUnpaidRefund() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createUnpaidOrder(userToken, "trade-service-invalid-state");

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/pay", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"paymentMode":"wechat"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code", is(5001)));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/refund", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code", is(5002)));
  }

  private long createUnpaidOrder(String userToken, String idempotencyKey) throws Exception {
    MvcResult result = mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {
                  "storeId":1,
                  "businessType":"takeaway",
                  "addressId":1,
                  "items":[{"itemId":1002,"quantity":2}],
                  "idempotencyKey":"%s"
                }
                """.formatted(idempotencyKey)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andReturn();
    return dataId(result);
  }

  private long dataId(MvcResult result) throws Exception {
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    return root.path("data").path("id").asLong();
  }
}
