package com.aituan.engagementplatform.file;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Component
class LskyProImageStorageClient implements ImageStorageClient {
  private final ImageStorageProperties.Lskypro properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private String cachedToken;

  LskyProImageStorageClient(ImageStorageProperties properties, ObjectMapper objectMapper) {
    this.properties = properties.getLskypro();
    this.objectMapper = objectMapper;
    this.restClient = RestClient.create();
  }

  @Override
  public StoredImage save(MultipartFile file, String bizType, String filename) {
    validateConfig();
    try {
      return upload(file, bizType, filename, resolveToken());
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 401 && canLogin()) {
        cachedToken = null;
        try {
          return upload(file, bizType, filename, resolveToken());
        } catch (RestClientException | IOException retryEx) {
          throw uploadFailure();
        }
      }
      throw uploadFailure();
    } catch (RestClientException | IOException ex) {
      throw uploadFailure();
    }
  }

  private StoredImage upload(MultipartFile file, String bizType, String filename, String token) throws IOException {
    String body = restClient.post()
        .uri(uploadUrl())
        .header("Authorization", "Bearer " + token)
        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(formData(file, filename))
        .retrieve()
        .body(String.class);
    JsonNode root = objectMapper.readTree(body);
    if (!root.path("status").asBoolean(false)) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "图床上传失败：" + root.path("message").asText("未知错误"));
    }
    JsonNode data = root.path("data");
    String publicUrl = data.path("links").path("url").asText("");
    String objectKey = data.path("key").asText("");
    if (!StringUtils.hasText(publicUrl)) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "图床上传失败：未返回图片地址");
    }
    if (!StringUtils.hasText(objectKey)) {
      objectKey = data.path("pathname").asText(filename);
    }
    return new StoredImage("lskypro", bizType + "/" + objectKey, publicUrl);
  }

  private String resolveToken() throws IOException {
    if (StringUtils.hasText(properties.getToken())) {
      return properties.getToken().trim();
    }
    if (StringUtils.hasText(cachedToken)) {
      return cachedToken;
    }
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("email", properties.getEmail().trim());
    form.add("password", properties.getPassword().trim());
    String body = restClient.post()
        .uri(tokenUrl())
        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(String.class);
    JsonNode root = objectMapper.readTree(body);
    if (!root.path("status").asBoolean(false)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "LskyPro 授权失败：" + root.path("message").asText("未知错误"));
    }
    String token = root.path("data").path("token").asText("");
    if (!StringUtils.hasText(token)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "LskyPro 授权失败：未返回 Token");
    }
    cachedToken = token;
    return token;
  }

  private void validateConfig() {
    if (!StringUtils.hasText(properties.getApiUrl()) || (!StringUtils.hasText(properties.getToken()) && !canLogin())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "LskyPro 图床配置不完整，请检查 .config");
    }
  }

  private boolean canLogin() {
    return StringUtils.hasText(properties.getEmail()) && StringUtils.hasText(properties.getPassword());
  }

  private String uploadUrl() {
    return baseUrl() + "/upload";
  }

  private String tokenUrl() {
    if (StringUtils.hasText(properties.getTokenUrl())) {
      String value = properties.getTokenUrl().trim();
      return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
    return baseUrl() + "/tokens";
  }

  private String baseUrl() {
    String apiUrl = properties.getApiUrl().trim();
    return apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
  }

  private BusinessException uploadFailure() {
    return new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "图床上传失败，请检查 .config 配置和网络");
  }

  private MultiValueMap<String, Object> formData(MultipartFile file, String filename) throws IOException {
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new ByteArrayResource(file.getBytes()) {
      @Override
      public String getFilename() {
        return filename;
      }
    });
    if (StringUtils.hasText(properties.getStrategyId())) {
      form.add("strategy_id", properties.getStrategyId().trim());
    }
    if (StringUtils.hasText(properties.getAlbumId())) {
      form.add("album_id", properties.getAlbumId().trim());
    }
    if (properties.getPermission() != null) {
      form.add("permission", properties.getPermission());
    }
    return form;
  }
}
