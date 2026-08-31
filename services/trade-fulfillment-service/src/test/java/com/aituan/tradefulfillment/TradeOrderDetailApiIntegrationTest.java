package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
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
class TradeOrderDetailApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void userCanReadOrderDetailDirectly() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createPaidTakeawayOrder(userToken, "detail-direct");

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/orders/{orderId}", orderId), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.orderNo").isString())
        .andExpect(jsonPath("$.data.orderKind").value("takeaway"))
        .andExpect(jsonPath("$.data.displayStatus").value("pending"))
        .andExpect(jsonPath("$.data.paymentStatus").value("paid"))
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("merchant_pending"))
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.items[0].itemId").value(1002))
        .andExpect(jsonPath("$.data.deliveryTimeline.currentStage").value("merchant_pending"));
  }

  @Test
  void returnsBusinessErrorForMissingOrderDetail() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/orders/{orderId}", 99999999L), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.NOT_FOUND));
  }

  private long createPaidTakeawayOrder(String userToken, String idempotencyKey) throws Exception {
    MvcResult created = mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"businessType":"takeaway","addressId":1,"items":[{"itemId":1002,"quantity":2}],"idempotencyKey":"%s"}
                """.formatted(idempotencyKey)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id", greaterThan(0)))
        .andReturn();
    long orderId = dataId(created);
    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/pay", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"paymentMode":"mock"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
    return orderId;
  }

  private long dataId(MvcResult result) throws Exception {
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    return root.path("data").path("id").asLong();
  }
}
