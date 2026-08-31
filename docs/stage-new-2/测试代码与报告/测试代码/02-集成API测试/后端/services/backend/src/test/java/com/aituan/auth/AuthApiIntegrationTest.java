package com.aituan.auth;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.exception.ErrorCode;
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
class AuthApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;

  @Test
  void userLoginShouldReturnTokenAndProfile() throws Exception {
    mockMvc.perform(post("/api/open/auth/user/login/password")
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"account":"demo_user","password":"123456"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andExpect(jsonPath("$.data.expiresIn", greaterThan(0)))
        .andExpect(jsonPath("$.data.profile.nickname").value("爱团用户"));
  }

  @Test
  void merchantAndAdminLoginShouldUseOwnRoleEndpoints() throws Exception {
    mockMvc.perform(post("/api/open/auth/merchant/login/password")
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"account":"demo_merchant","password":"123456"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())));

    mockMvc.perform(post("/api/open/auth/admin/login/password")
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"account":"demo_admin","password":"123456"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())));
  }

  @Test
  void merchantLoginShouldRejectUserAccount() throws Exception {
    mockMvc.perform(post("/api/open/auth/merchant/login/password")
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"account":"demo_user","password":"123456"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.FORBIDDEN));
  }

  @Test
  void wrongPasswordShouldReturnBusinessError() throws Exception {
    mockMvc.perform(post("/api/open/auth/user/login/password")
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"account":"demo_user","password":"wrong-password"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.INVALID_PASSWORD));
  }

  @Test
  void blankLoginFieldsShouldReturnBadRequest() throws Exception {
    mockMvc.perform(post("/api/open/auth/user/login/password")
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"account":"","password":""}
                """))
        .andExpect(ApiTestSupport.badRequest());
  }

  @Test
  void tokenCheckShouldReturnValidOnlyWithBearerToken() throws Exception {
    mockMvc.perform(get("/api/open/auth/token/check"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.valid").value(false));

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/open/auth/token/check"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.valid").value(true))
        .andExpect(jsonPath("$.data.profile.nickname").value("爱团用户"));
  }
}
