package com.aituan.common.file;

import static com.aituan.common.jdbc.JdbcGeneratedKeys.insertAndReturnId;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
  private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

  private final JdbcTemplate jdbcTemplate;
  private final ImageStorageProperties properties;
  private final LocalImageStorageClient localImageStorageClient;
  private final LskyProImageStorageClient lskyProImageStorageClient;

  public FileStorageService(
      JdbcTemplate jdbcTemplate,
      ImageStorageProperties properties,
      LocalImageStorageClient localImageStorageClient,
      LskyProImageStorageClient lskyProImageStorageClient) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
    this.localImageStorageClient = localImageStorageClient;
    this.lskyProImageStorageClient = lskyProImageStorageClient;
  }

  public FileAssetView save(MultipartFile file, String bizType) {
    CurrentUser current = CurrentUserContext.required();
    String normalizedBizType = normalizeBizType(bizType);
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    validate(file, contentType);

    String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
    String filename = UUID.randomUUID() + (extension == null || extension.isBlank() ? defaultExtension(contentType) : "." + extension.toLowerCase(Locale.ROOT));
    StoredImage stored = storageClient().save(file, normalizedBizType, filename);
    try {
      long id = insertAndReturnId(
          jdbcTemplate,
          """
          insert into file_asset(owner_type, owner_id, biz_type, original_name, storage_type, object_key, public_url, mime_type, size_bytes)
          values (?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          current.accountType().name().toLowerCase(Locale.ROOT),
          current.accountId(),
          normalizedBizType,
          file.getOriginalFilename() == null ? filename : file.getOriginalFilename(),
          stored.storageType(),
          stored.objectKey(),
          stored.publicUrl(),
          contentType,
          file.getSize());
      return new FileAssetView(id, normalizedBizType, file.getOriginalFilename(), stored.publicUrl(), contentType, file.getSize(), LocalDateTime.now());
    } catch (RuntimeException ex) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "图片记录保存失败，请检查图片地址长度或数据库配置");
    }
  }

  public Resource load(String objectKey) {
    Path target = localImageStorageClient.rootDir().resolve(objectKey).normalize();
    if (!target.startsWith(localImageStorageClient.rootDir()) || !Files.isRegularFile(target)) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    return new FileSystemResource(target);
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

  private ImageStorageClient storageClient() {
    String strategy = properties.getStrategy() == null ? "local" : properties.getStrategy().trim().toLowerCase(Locale.ROOT);
    return switch (strategy) {
      case "local", "" -> localImageStorageClient;
      case "lskypro" -> lskyProImageStorageClient;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "图片上传策略不正确");
    };
  }

  private String normalizeBizType(String bizType) {
    String value = bizType == null ? "" : bizType.trim().toLowerCase(Locale.ROOT);
    return switch (value) {
      case "avatar", "store", "item", "announcement", "seed", "merchant-certification" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "图片业务类型不正确");
    };
  }

  private String defaultExtension(String contentType) {
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> ".jpg";
    };
  }
}
