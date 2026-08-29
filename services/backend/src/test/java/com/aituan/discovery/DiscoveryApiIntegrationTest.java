package com.aituan.discovery;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class DiscoveryApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void homeShouldExposeModulesAndRecommendationsWithoutLogin() throws Exception {
    mockMvc.perform(get("/api/app/discovery/home"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.modules.length()").value(greaterThanOrEqualTo(8)))
        .andExpect(jsonPath("$.data.modules[0].code").exists())
        .andExpect(jsonPath("$.data.recommendations.list").exists())
        .andExpect(jsonPath("$.data.unreadMessageCount").value(0));
  }

  @Test
  void moduleShouldFilterStoresAndItemsByBusinessType() throws Exception {
    mockMvc.perform(get("/api/app/discovery/modules/takeaway"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.moduleCode").value("takeaway"))
        .andExpect(jsonPath("$.data.businessType").value("takeaway"))
        .andExpect(jsonPath("$.data.stores[0].businessType").value("takeaway"))
        .andExpect(jsonPath("$.data.featuredItems.list[0].businessType").value("takeaway"));
  }

  @Test
  void searchShouldSupportKeywordAndBusinessTypeFilters() throws Exception {
    mockMvc.perform(get("/api/app/discovery/stores/search")
            .param("keyword", "塔斯汀")
            .param("businessType", "takeaway")
            .param("page", "1")
            .param("pageSize", "5"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list").exists())
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.pageSize").value(5))
        .andExpect(jsonPath("$.data.total").exists());
  }
}
