package com.aituan.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityBoundaryApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;

  @Test
  void protectedAppApiRequiresUserToken() throws Exception {
    mockMvc.perform(get("/api/app/account/profile"))
        .andExpect(ApiTestSupport.unauthorized());

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/app/account/profile"),
            ApiTestSupport.merchantToken(jwtTokenService)))
        .andExpect(ApiTestSupport.forbidden());

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/app/account/profile"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
  }

  @Test
  void merchantApiRejectsUserAndAllowsMerchantToken() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/profile/me"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(ApiTestSupport.forbidden());

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/profile/me"),
            ApiTestSupport.merchantToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
  }

  @Test
  void adminApiRejectsUserAndAllowsAdminToken() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/admin/dashboard"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(ApiTestSupport.forbidden());

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/admin/dashboard"),
            ApiTestSupport.adminToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
  }

  @Test
  void publicOpenAndDiscoveryApisDoNotRequireToken() throws Exception {
    mockMvc.perform(get("/api/open/auth/token/check"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());

    mockMvc.perform(get("/api/app/discovery/home"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
  }
}
