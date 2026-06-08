package com.aituan.common.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.aituan.TestAuthSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "aituan.upload.root-dir=target/test-uploads")
class FileStorageServiceTest {
  @Autowired private FileStorageService fileStorageService;

  @AfterEach
  void cleanup() {
    TestAuthSupport.clear();
  }

  @Test
  void reviewComplaintAndReportImagesCanBeUploaded() {
    TestAuthSupport.loginAsUser(1L, 1L);

    for (String bizType : java.util.List.of("review", "complaint", "report")) {
      FileAssetView asset = fileStorageService.save(
          new MockMultipartFile("file", bizType + ".png", "image/png", tinyPng()),
          bizType);

      assertThat(asset.bizType()).isEqualTo(bizType);
      assertThat(asset.publicUrl()).isNotBlank();
    }
  }

  private byte[] tinyPng() {
    return java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");
  }
}
