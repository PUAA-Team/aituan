package com.aituan.trade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;

  @Test
  void userCanListAndOpenOwnOrders() throws Exception {
    String token = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/orders"), token))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list[0].orderNo").exists());

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/orders/9011"), token))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(9011));
  }

  @Test
  void payOrderShouldValidatePaymentMode() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/app/trade/orders/9011/pay"),
            ApiTestSupport.userToken(jwtTokenService))
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"paymentMode":""}
                """))
        .andExpect(ApiTestSupport.badRequest());
  }

  @Test
  void merchantAndAdminCanUseOpsOrderEndpointsWithOwnRoles() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/trade/orders"),
            ApiTestSupport.merchantToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list").exists());

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/admin/trade/orders"),
            ApiTestSupport.adminToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list").exists());
  }

  @Test
  void userCannotUseMerchantOrAdminTradeOpsEndpoints() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/merchant/trade/orders"), userToken))
        .andExpect(ApiTestSupport.forbidden());

    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/trade/orders"), userToken))
        .andExpect(ApiTestSupport.forbidden());
  }
}
