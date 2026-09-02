package com.aituan.engagementplatform.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class FileStorageServiceTest {
  @TempDir Path tempDir;
  private FileStorageService service;

  @BeforeEach
  void setUp() {
    ImageStorageProperties properties = new ImageStorageProperties();
    properties.setRootDir(tempDir.toString());
    properties.setMaxSizeBytes(4);
    LocalImageStorageClient local = new LocalImageStorageClient(properties);
    service = new FileStorageService(
        mock(JdbcTemplate.class), properties, local,
        new LskyProImageStorageClient(properties, new ObjectMapper()));
    CurrentUser current = new CurrentUser(1L, 1L, AccountType.USER, "测试用户");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(current, null));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void rejectsUnsupportedMimeType() {
    MockMultipartFile file = new MockMultipartFile("file", "a.svg", "image/svg+xml", new byte[] {1});
    assertThatThrownBy(() -> service.save(file, "review"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("仅支持");
  }

  @Test
  void rejectsOversizedFile() {
    MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3, 4, 5});
    assertThatThrownBy(() -> service.save(file, "review"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("5MB");
  }

  @Test
  void rejectsUnknownBusinessType() {
    MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});
    assertThatThrownBy(() -> service.save(file, "../../outside"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("业务类型");
  }

  @Test
  void blocksPathTraversalOnRead() {
    assertThatThrownBy(() -> service.load("../secret.txt"))
        .isInstanceOf(BusinessException.class);
  }
}
