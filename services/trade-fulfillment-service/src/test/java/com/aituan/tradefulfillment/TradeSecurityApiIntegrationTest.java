package com.aituan.tradefulfillment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.aituan.ApiTestSupport;
import com.aituan.common.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TradeSecurityApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Test
  void appTradeApiRequiresUserRole() throws Exception {
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);

    mockMvc.perform(get("/api/app/trade/cart").param("storeId", "1"))
        .andExpect(ApiTestSupport.unauthorized());

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/cart").param("storeId", "1"), merchantToken))
        .andExpect(ApiTestSupport.forbidden());
  }

  @Test
  void merchantTradeApiRequiresMerchantRole() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String adminToken = ApiTestSupport.adminToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/merchant/trade/orders"), userToken))
        .andExpect(ApiTestSupport.forbidden());

    mockMvc.perform(ApiTestSupport.bearer(get("/api/merchant/trade/orders"), adminToken))
        .andExpect(ApiTestSupport.forbidden());
  }

  @Test
  void adminTradeApiRequiresAdminRole() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/trade/orders"), userToken))
        .andExpect(ApiTestSupport.forbidden());

    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/trade/orders"), merchantToken))
        .andExpect(ApiTestSupport.forbidden());
  }

  @Test
  void adminDeliveryApiRequiresAdminRole() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/delivery/tasks"), userToken))
        .andExpect(ApiTestSupport.forbidden());

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/delivery/tasks/{taskId}/pause", 1L), merchantToken))
        .andExpect(ApiTestSupport.forbidden());
  }

  @Test
  void validatesTradeRequestParameters() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/cart").param("storeId", "0"), userToken))
        .andExpect(ApiTestSupport.badRequest());

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/orders").param("page", "0"), userToken))
        .andExpect(ApiTestSupport.badRequest());

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/pay", 1L), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("{}"))
        .andExpect(ApiTestSupport.badRequest());
  }
}
