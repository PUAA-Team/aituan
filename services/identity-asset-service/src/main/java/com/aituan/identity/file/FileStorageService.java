package com.aituan.identity.file;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.api.RequestIds;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
  private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

  private final IdentityUploadProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient platformClient;

  public FileStorageService(IdentityUploadProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(1000);
    requestFactory.setReadTimeout(5000);
    this.platformClient = RestClient.builder()
        .baseUrl(properties.getPlatformBaseUrl())
        .requestFactory(requestFactory)
        .build();
  }

  public FileAssetView save(MultipartFile file, String bizType) {
    String normalizedBizType = normalizeBizType(bizType);
    String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    validate(file, contentType);
    if (!"local".equalsIgnoreCase(properties.getStrategy())) {
      return saveThroughPlatform(file, normalizedBizType);
    }

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

  private FileAssetView saveThroughPlatform(MultipartFile file, String bizType) {
    try {
      LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", file.getResource());
      body.add("bizType", bizType);
      JsonNode root = platformClient.post()
          .uri("/api/common/files/upload")
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .headers(headers -> {
            headers.set("X-Request-Id", RequestIds.current());
            String authorization = currentAuthorization();
            if (StringUtils.hasText(authorization)) headers.set(HttpHeaders.AUTHORIZATION, authorization);
          })
          .body(body)
          .retrieve()
          .body(JsonNode.class);
      if (root == null || root.path("code").asInt(-1) != 0 || root.path("data").isMissingNode()) {
        throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "平台文件服务返回异常");
      }
      return objectMapper.treeToValue(root.path("data"), FileAssetView.class);
    } catch (BusinessException exception) {
      throw exception;
    } catch (RestClientException | IOException exception) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "平台文件服务暂不可用");
    }
  }

  private String currentAuthorization() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
    return null;
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
