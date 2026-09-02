package com.aituan.identity.account;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AccountAssetControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void profileShouldBeReadableAndUpdatable() throws Exception {
    String token = loginUser();

    mockMvc.perform(get("/api/app/account/profile")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.userId").value(5001));

    mockMvc.perform(put("/api/app/account/profile")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nickname":"爱团用户-测试","avatarUrl":"https://static.example/avatar.png"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.nickname").value("爱团用户-测试"));
  }

  @Test
  void addressesShouldSupportListCreateDefaultAndDelete() throws Exception {
    String token = loginUser();

    mockMvc.perform(get("/api/app/account/addresses")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

    Number addressId = JsonPath.read(mockMvc.perform(post("/api/app/account/addresses")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "contactName":"测试收货人",
                  "contactPhone":"13800009999",
                  "province":"北京市",
                  "city":"北京市",
                  "district":"海淀区",
                  "detailAddress":"测试路 1 号",
                  "longitude":116.352,
                  "latitude":39.984,
                  "tagName":"测试",
                  "isDefault":false,
                  "deliveryNote":"放前台"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.contactName").value("测试收货人"))
        .andReturn().getResponse().getContentAsString(), "$.data.id");

    mockMvc.perform(post("/api/app/account/addresses/{addressId}/default", addressId.longValue())
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc.perform(delete("/api/app/account/addresses/{addressId}", addressId.longValue())
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  @Test
  void favoritesShouldSupportListCreateAndDelete() throws Exception {
    String token = loginUser();
    long targetId = 990000L + (System.nanoTime() % 100000L);

    mockMvc.perform(get("/api/app/account/favorites")
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)));

    mockMvc.perform(post("/api/app/account/favorites")
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "favoriteType":"store",
                  "targetId":%d,
                  "targetName":"测试收藏门店",
                  "coverUrl":"",
                  "subtitle":"接口测试收藏"
                }
                """.formatted(targetId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.targetId").value(targetId));

    mockMvc.perform(delete("/api/app/account/favorites/store/{targetId}", targetId)
            .header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
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
