package com.aituan.identity.file;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
  private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

  private final IdentityUploadProperties properties;

  public FileStorageService(IdentityUploadProperties properties) {
    this.properties = properties;
  }

  public FileAssetView save(MultipartFile file, String bizType) {
    String normalizedBizType = normalizeBizType(bizType);
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    validate(file, contentType);

    String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
    String filename = UUID.randomUUID() + (extension == null || extension.isBlank() ? defaultExtension(contentType) : "." + extension.toLowerCase(Locale.ROOT));
    String objectKey = normalizedBizType + "/" + filename;
    Path root = Path.of(properties.getRootDir()).toAbsolutePath().normalize();
    Path target = root.resolve(objectKey).normalize();
    if (!target.startsWith(root)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "图片路径不正确");
    }
    try {
      Files.createDirectories(target.getParent());
      file.transferTo(target);
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "图片保存失败");
    }
    String publicPrefix = properties.getPublicPrefix() == null ? "/api/common/files" : properties.getPublicPrefix();
    String publicUrl = publicPrefix.replaceAll("/+$", "") + "/" + objectKey.replace('\\', '/');
    return new FileAssetView(null, normalizedBizType, file.getOriginalFilename(), publicUrl, contentType, file.getSize(), LocalDateTime.now());
  }

  private void validate(MultipartFile file, String contentType) {
    if (file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
    }
    if (file.getSize() > properties.getMaxSizeBytes()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "图片大小不能超过 5MB");
    }
    if (!ALLOWED_TYPES.contains(contentType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 JPG、PNG、WEBP 图片");
    }
  }

  private String normalizeBizType(String bizType) {
    String value = bizType == null ? "" : bizType.trim().toLowerCase(Locale.ROOT);
    if ("avatar".equals(value)) {
      return value;
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "图片业务类型不正确");
  }

  private String defaultExtension(String contentType) {
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> ".jpg";
    };
  }
}
