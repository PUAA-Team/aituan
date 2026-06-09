package com.aituan.coupon;

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
class CouponApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;

  @Test
  void userCanListOwnCouponsAndAvailableTemplates() throws Exception {
    String token = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/app/account/coupons").param("status", "usable"),
            token))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data[0].name").exists())
        .andExpect(jsonPath("$.data[0].status").value("unused"));

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/account/coupons/available"), token))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data[0].templateId").exists())
        .andExpect(jsonPath("$.data[0].claimable").exists());
  }

  @Test
  void usableForOrderShouldReturnCouponOptionsForAmount() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/app/account/coupons/usable-for-order").param("orderAmount", "60.00"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data[0].userCouponId").exists())
        .andExpect(jsonPath("$.data[0].usable").isBoolean());
  }

  @Test
  void claimCouponRequiresLogin() throws Exception {
    mockMvc.perform(post("/api/app/account/coupons/1/claim"))
        .andExpect(ApiTestSupport.unauthorized());
  }
}
