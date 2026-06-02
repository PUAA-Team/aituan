package com.aituan.stage6;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage6AccountAssetApiIntegrationTest {
  private static final String PASSWORD = "123456";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void adminProfileAllowsAdminAndRejectsUser() throws Exception {
    String adminToken = login("/api/open/auth/admin/login/password", "demo_admin");
    mockMvc.perform(get("/api/admin/account/profile")
            .header("Authorization", bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accountNo").value("A202605170001"))
        .andExpect(jsonPath("$.data.accountType").value("ADMIN"))
        .andExpect(jsonPath("$.data.nickname").value("demo_admin"));

    String userToken = login("/api/open/auth/user/login/password", "demo_user");
    mockMvc.perform(get("/api/admin/account/profile")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(2002));
  }

  @Test
  void memberInfoShowsDemoUserGrowthProgress() throws Exception {
    String userToken = login("/api/open/auth/user/login/password", "demo_user");

    mockMvc.perform(get("/api/app/account/member/info")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.currentLevelCode").value("NORMAL"))
        .andExpect(jsonPath("$.data.currentLevelName").value("普通会员"))
        .andExpect(jsonPath("$.data.growthValue").value(128))
        .andExpect(jsonPath("$.data.nextLevelName").value("银卡会员"))
        .andExpect(jsonPath("$.data.growthToNextLevel").value(172))
        .andExpect(jsonPath("$.data.progressPercent").value(43))
        .andExpect(jsonPath("$.data.benefits[0].title").value("基础服务"));
  }

  @Test
  void couponApisExposeDemoCouponsAndOrderOptions() throws Exception {
    String userToken = login("/api/open/auth/user/login/password", "demo_user");

    mockMvc.perform(get("/api/app/account/coupons")
            .param("status", "usable")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data", hasSize(2)))
        .andExpect(jsonPath("$.data[0].id").value(9002))
        .andExpect(jsonPath("$.data[0].name").value("满50减10"))
        .andExpect(jsonPath("$.data[1].id").value(9001))
        .andExpect(jsonPath("$.data[1].name").value("满30减5"));

    mockMvc.perform(get("/api/app/account/coupons/available")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data[0].templateId").value(1))
        .andExpect(jsonPath("$.data[0].claimable").value(false))
        .andExpect(jsonPath("$.data[0].reason").value("已达领取上限"));

    mockMvc.perform(get("/api/app/account/coupons/usable-for-order")
            .param("orderAmount", "60")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data", hasSize(2)))
        .andExpect(jsonPath("$.data[0].userCouponId").value(9002))
        .andExpect(jsonPath("$.data[0].usable").value(true))
        .andExpect(jsonPath("$.data[0].discountAmount").value(10.00))
        .andExpect(jsonPath("$.data[1].userCouponId").value(9001))
        .andExpect(jsonPath("$.data[1].usable").value(true))
        .andExpect(jsonPath("$.data[1].discountAmount").value(5.00));
  }

  @Test
  void couponClaimStopsAtTemplateInventoryLimit() throws Exception {
    String adminToken = login("/api/open/auth/admin/login/password", "demo_admin");
    MvcResult created = mockMvc.perform(post("/api/admin/operation/coupon-templates")
            .header("Authorization", bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "name", "库存边界测试券",
                "type", "full_reduction",
                "faceValue", "3.00",
                "thresholdAmount", "20.00",
                "businessScope", "all",
                "validKind", "relative",
                "validDays", 30,
                "totalQty", 1,
                "perUserLimit", 10,
                "status", "enabled"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.totalQty").value(1))
        .andExpect(jsonPath("$.data.issuedQty").value(0))
        .andReturn();
    long templateId = objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asLong();

    String userToken = login("/api/open/auth/user/login/password", "demo_user");
    mockMvc.perform(post("/api/app/account/coupons/" + templateId + "/claim")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc.perform(post("/api/app/account/coupons/" + templateId + "/claim")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(5001))
        .andExpect(jsonPath("$.message").value("优惠券已领完"));

    mockMvc.perform(get("/api/admin/operation/coupon-templates")
            .header("Authorization", bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.id == " + templateId + ")].issuedQty").value(1));
  }

  @Test
  void messageReadApiMarksSingleMessageRead() throws Exception {
    String userToken = login("/api/open/auth/user/login/password", "demo_user");

    mockMvc.perform(get("/api/app/message/station")
            .param("type", "order")
            .param("pageSize", "20")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.total").value(6))
        .andExpect(jsonPath("$.data.list[?(@.id == 2)].unread").value(true));

    mockMvc.perform(patch("/api/app/message/station/2/read")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc.perform(get("/api/app/message/station")
            .param("type", "order")
            .param("pageSize", "20")
            .header("Authorization", bearer(userToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.list[?(@.id == 2)].unread").value(false));
  }

  private String login(String path, String account) throws Exception {
    MvcResult result = mockMvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "account", account,
                "password", PASSWORD))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/token").asText();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
