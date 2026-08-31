package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
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
class VoucherBookingApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void userCanCreateServiceOrderVoucherAndBooking() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long orderId = createServiceOrder(userToken);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/pay", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"paymentMode":"mock"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.displayStatus").value("unused"))
        .andExpect(jsonPath("$.data.paymentStatus").value("paid"))
        .andExpect(jsonPath("$.data.voucher.voucherCode").isString())
        .andExpect(jsonPath("$.data.voucher.status").value("unused"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/booking", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {
                  "contactName":"李同学",
                  "contactPhone":"18800001111",
                  "bookingDate":"2026-09-01",
                  "bookingTimeSlot":"18:00-20:00",
                  "guestCount":2,
                  "remark":"靠窗"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.orderId").value(orderId))
        .andExpect(jsonPath("$.data.businessType").value("group_buy"))
        .andExpect(jsonPath("$.data.contactName").value("李同学"))
        .andExpect(jsonPath("$.data.guestCount").value(2))
        .andExpect(jsonPath("$.data.storeConfirmStatus").value("pending"));

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/orders/{orderId}/booking", orderId), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.orderId").value(orderId))
        .andExpect(jsonPath("$.data.bookingTimeSlot").value("18:00-20:00"));
  }

  private long createServiceOrder(String userToken) throws Exception {
    MvcResult result = mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {
                  "storeId":2,
                  "businessType":"group_buy",
                  "items":[{"itemId":2001,"quantity":1}],
                  "idempotencyKey":"trade-service-voucher-booking"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id", greaterThan(0)))
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andReturn();
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    return root.path("data").path("id").asLong();
  }
}
