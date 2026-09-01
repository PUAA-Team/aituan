package com.aituan.engagementplatform.file;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
class LocalImageStorageClient implements ImageStorageClient {
  private final Path rootDir;
  private final String publicPrefix;

  LocalImageStorageClient(ImageStorageProperties properties) {
    this.rootDir = Path.of(properties.getRootDir()).toAbsolutePath().normalize();
    String prefix = properties.getPublicPrefix();
    this.publicPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
  }

  @Override
  public StoredImage save(MultipartFile file, String bizType, String filename) {
    Path bizDir = rootDir.resolve(bizType).normalize();
    Path target = bizDir.resolve(filename).normalize();
    if (!target.startsWith(rootDir)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "文件路径非法");
    }
    try {
      Files.createDirectories(bizDir);
      file.transferTo(target);
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "图片保存失败");
    }
    String objectKey = bizType + "/" + filename;
    return new StoredImage("local", objectKey, publicPrefix + "/" + objectKey);
  }

  Path rootDir() {
    return rootDir;
  }
}

