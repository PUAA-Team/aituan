package com.aituan.identity.asset;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "aituan.internal.trade-base-url=http://127.0.0.1:9")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAssetFeatureControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void couponsShouldSupportListAvailableAndOrderCalculation() throws Exception {
    String token = loginUser();

    mockMvc.perform(get("/api/app/account/coupons")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

    mockMvc.perform(get("/api/app/account/coupons/available")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

    mockMvc.perform(get("/api/app/account/coupons/usable-for-order")
            .header("Authorization", bearer(token))
            .param("orderAmount", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));
  }

  @Test
  void couponClaimBoundaryShouldReturnBusinessFailure() throws Exception {
    String token = loginUser();

    mockMvc.perform(post("/api/app/account/coupons/1/claim")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(5001));
  }

  @Test
  void memberAndMessagesShouldBeAvailable() throws Exception {
    String token = loginUser();

    mockMvc.perform(get("/api/app/account/member/info")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.currentLevelName").isNotEmpty());

    MvcResult messages = mockMvc.perform(get("/api/app/message/station")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
        .andReturn();
    Number messageId = JsonPath.read(messages.getResponse().getContentAsString(), "$.data.list[0].id");

    mockMvc.perform(patch("/api/app/message/station/{messageId}/read", messageId.longValue())
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc.perform(patch("/api/app/message/station/batch-read")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"messageIds":[6001,6002]}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  @Test
  void emptyMessageBatchShouldReturnValidationFailure() throws Exception {
    String token = loginUser();

    mockMvc.perform(patch("/api/app/message/station/batch-read")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"messageIds":[]}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(1000));
  }

  private String loginUser() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/open/auth/user/login/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"account":"demo_user","password":"123456"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
