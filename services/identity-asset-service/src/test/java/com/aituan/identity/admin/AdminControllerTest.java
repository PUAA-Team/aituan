package com.aituan.identity.admin;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AdminControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void adminUsersAndProfileBasePathsShouldWork() throws Exception {
    String token = loginAdmin();

    mockMvc.perform(get("/api/admin/users")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)));

    mockMvc.perform(get("/api/admin/account/profile")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.accountNo").value("A202605170001"));
  }

  @Test
  void memberLevelBasePathShouldSupportListAndCreate() throws Exception {
    String token = loginAdmin();
    String levelCode = "TEST_" + System.nanoTime();

    mockMvc.perform(get("/api/admin/operation/member-levels")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

    mockMvc.perform(post("/api/admin/operation/member-levels")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "levelCode":"%s",
                  "levelName":"测试会员等级",
                  "minGrowthValue":99999,
                  "benefits":[{"title":"测试权益","desc":"用于接口测试"}],
                  "iconUrl":"",
                  "color":"#000000",
                  "sortOrder":99,
                  "status":"enabled"
                }
                """.formatted(levelCode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.levelCode").value(levelCode));
  }

  @Test
  void couponTemplateBasePathShouldSupportListAndCreate() throws Exception {
    String token = loginAdmin();
    String templateName = "测试优惠券模板-" + System.nanoTime();

    mockMvc.perform(get("/api/admin/operation/coupon-templates")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

    mockMvc.perform(post("/api/admin/operation/coupon-templates")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name":"%s",
                  "type":"full_reduction",
                  "faceValue":5.00,
                  "thresholdAmount":20.00,
                  "businessScope":"all",
                  "validKind":"relative",
                  "validDays":7,
                  "totalQty":100,
                  "perUserLimit":1,
                  "status":"enabled"
                }
                """.formatted(templateName)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.name").value(templateName));
  }

  private String loginAdmin() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/open/auth/admin/login/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"account":"demo_admin","password":"123456"}
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
