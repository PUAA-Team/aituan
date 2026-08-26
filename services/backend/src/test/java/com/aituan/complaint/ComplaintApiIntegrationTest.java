package com.aituan.complaint;

import static org.hamcrest.Matchers.containsString;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplaintApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;

  @Test
  void userCanListAndOpenOwnComplaintTickets() throws Exception {
    String token = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/complaints"), token))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list[0].ticketNo").exists());

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/complaints/1"), token))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.complaint.id").value(1));
  }

  @Test
  void submitComplaintShouldValidateRequiredFields() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/complaints"), ApiTestSupport.userToken(jwtTokenService))
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"orderId":9011,"category":"","title":"","detail":""}
                """))
        .andExpect(ApiTestSupport.badRequest())
        .andExpect(jsonPath("$.message", containsString("title")));
  }

  @Test
  @Transactional
  void userCanSupplementOpenComplaint() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/app/complaints/1/supplements"),
            ApiTestSupport.userToken(jwtTokenService))
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"content":"请继续跟进骑手联系情况"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.logs[?(@.action == 'supplement')]").exists());
  }

  @Test
  void merchantCanListOwnComplaintsButUserCannotAccessMerchantEndpoint() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/ops/complaints"),
            ApiTestSupport.merchantToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list").exists());

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/ops/complaints"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(ApiTestSupport.forbidden());
  }

  @Test
  void adminComplaintActionShouldUseBusinessStateValidation() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/admin/governance/complaints/1/resolve"),
            ApiTestSupport.adminToken(jwtTokenService))
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"remark":"跳过受理直接完成"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(5002));
  }
}
