package com.aituan.identity.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "aituan.internal.trade-base-url=http://127.0.0.1:9")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;

  @Test
  void tokenCheckWithoutTokenShouldReturnInvalid() throws Exception {
    mockMvc.perform(get("/api/open/auth/token/check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.valid").value(false));
  }

  @Test
  void seededUserShouldLoginWithPassword() throws Exception {
    mockMvc.perform(post("/api/open/auth/user/login/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"account":"demo_user","password":"123456"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.token").isNotEmpty())
        .andExpect(jsonPath("$.data.profile.id").value(5001));
  }

  @Test
  void wrongPasswordShouldReturnBusinessFailure() throws Exception {
    mockMvc.perform(post("/api/open/auth/user/login/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"account":"demo_user","password":"wrong-password"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(3003));
  }

  @Test
  void appApiWithoutTokenShouldReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/api/app/account/profile"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(2001));
  }
}
