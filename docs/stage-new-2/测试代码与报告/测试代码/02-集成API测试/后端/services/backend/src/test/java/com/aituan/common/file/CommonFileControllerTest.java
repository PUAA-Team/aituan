package com.aituan.common.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "aituan.upload.strategy=local",
    "aituan.upload.root-dir=target/test-api-uploads",
    "aituan.upload.public-prefix=/api/common/files"
})
class CommonFileControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void multipartUploadShouldReturnAssetAndUploadedFileCanBeRead() throws Exception {
    MvcResult upload = mockMvc.perform(ApiTestSupport.bearer(
            multipart("/api/common/files/upload")
                .file(new MockMultipartFile("file", "review.png", "image/png", tinyPng()))
                .param("bizType", "review"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.bizType").value("review"))
        .andExpect(jsonPath("$.data.publicUrl").isNotEmpty())
        .andReturn();

    JsonNode body = objectMapper.readTree(upload.getResponse().getContentAsString());
    String publicUrl = body.path("data").path("publicUrl").asText();
    assertThat(publicUrl).startsWith("/api/common/files/review/");

    mockMvc.perform(get(publicUrl))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable"));
  }

  @Test
  void invalidBizTypeShouldReturnBusinessBadRequest() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            multipart("/api/common/files/upload")
                .file(new MockMultipartFile("file", "bad.png", "image/png", tinyPng()))
                .param("bizType", "unknown-biz"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(com.aituan.common.exception.ErrorCode.BAD_REQUEST));
  }

  private byte[] tinyPng() {
    return Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");
  }
}
