package com.aituan.common.file;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
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
  private final Path rootDir;
  private final String publicPrefix;
  private final long maxSizeBytes;

  public FileStorageService(
      JdbcTemplate jdbcTemplate,
      @Value("${aituan.upload.root-dir:D:/aituan_runtime/uploads}") String rootDir,
      @Value("${aituan.upload.public-prefix:/api/common/files}") String publicPrefix,
      @Value("${aituan.upload.max-size-bytes:5242880}") long maxSizeBytes) {
    this.jdbcTemplate = jdbcTemplate;
    this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
    this.publicPrefix = publicPrefix.endsWith("/") ? publicPrefix.substring(0, publicPrefix.length() - 1) : publicPrefix;
    this.maxSizeBytes = maxSizeBytes;
  }

  public FileAssetView save(MultipartFile file, String bizType) {
    CurrentUser current = CurrentUserContext.required();
    String normalizedBizType = normalizeBizType(bizType);
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    if (file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件不能为空");
    }
    if (file.getSize() > maxSizeBytes) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "图片大小不能超过 5MB");
    }
    if (!ALLOWED_TYPES.contains(contentType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 JPG、PNG、WEBP 图片");
    }

    String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
    String filename = UUID.randomUUID() + (extension == null || extension.isBlank() ? defaultExtension(contentType) : "." + extension.toLowerCase(Locale.ROOT));
    Path bizDir = rootDir.resolve(normalizedBizType).normalize();
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

    String objectKey = normalizedBizType + "/" + filename;
    String publicUrl = publicPrefix + "/" + objectKey;
    jdbcTemplate.update(
        """
        insert into file_asset(owner_type, owner_id, biz_type, original_name, storage_type, object_key, public_url, mime_type, size_bytes)
        values (?, ?, ?, ?, 'local', ?, ?, ?, ?)
        """,
        current.accountType().name().toLowerCase(Locale.ROOT),
        current.accountId(),
        normalizedBizType,
        file.getOriginalFilename() == null ? filename : file.getOriginalFilename(),
        objectKey,
        publicUrl,
        contentType,
        file.getSize());
    Long id = jdbcTemplate.queryForObject("select max(id) from file_asset where object_key = ?", Long.class, objectKey);
    return new FileAssetView(id, normalizedBizType, file.getOriginalFilename(), publicUrl, contentType, file.getSize(), LocalDateTime.now());
  }

  public Resource load(String objectKey) {
    Path target = rootDir.resolve(objectKey).normalize();
    if (!target.startsWith(rootDir) || !Files.isRegularFile(target)) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    return new FileSystemResource(target);
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
