package com.aituan.merchantcatalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;

@Service
class FileStorageService {
  private final RestClient platformClient;

  FileStorageService(
      @Value("${aituan.services.platform-url:http://engagement-platform-service:8084}") String platformUrl) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(1000);
    requestFactory.setReadTimeout(5000);
    this.platformClient = RestClient.builder().baseUrl(platformUrl).requestFactory(requestFactory).build();
  }

  FileAssetView save(MultipartFile file, String bizType) {
    String type = bizType == null || bizType.isBlank() ? "merchant" : bizType.trim();
    try {
      LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", file.getResource());
      body.add("bizType", type);
      JsonNode root = platformClient.post()
          .uri("/api/common/files/upload")
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .headers(headers -> {
            headers.set("X-Request-Id", com.aituan.common.api.RequestIds.current());
            String authorization = currentAuthorization();
            if (StringUtils.hasText(authorization)) headers.set(HttpHeaders.AUTHORIZATION, authorization);
          })
          .body(body)
          .retrieve()
          .body(JsonNode.class);
      if (root == null || root.path("code").asInt(-1) != 0 || !root.path("data").hasNonNull("publicUrl")) {
        throw new com.aituan.common.exception.BusinessException(
            com.aituan.common.exception.ErrorCode.BUSINESS_RULE_VIOLATION, "平台文件服务返回异常");
      }
      return new FileAssetView(root.path("data").path("publicUrl").asText());
    } catch (com.aituan.common.exception.BusinessException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new com.aituan.common.exception.BusinessException(
          com.aituan.common.exception.ErrorCode.BUSINESS_RULE_VIOLATION, "平台文件服务暂不可用");
    }
  }

  private String currentAuthorization() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
    return null;
  }

  record FileAssetView(String publicUrl) {}
}

@Service
class MapDistanceService {
  private final org.springframework.web.client.RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String provider;
  private final String amapKey;
  private final String amapGeocodeUrl;
  private final String amapDistanceUrl;
  private final String amapRegeoUrl;

  MapDistanceService(
      ObjectMapper objectMapper,
      @Value("${aituan.map.provider:local}") String provider,
      @Value("${aituan.map.amap.web-api-key:}") String amapKey,
      @Value("${aituan.map.amap.geocode-url:https://restapi.amap.com/v3/geocode/geo}") String amapGeocodeUrl,
      @Value("${aituan.map.amap.distance-url:https://restapi.amap.com/v3/distance}") String amapDistanceUrl,
      @Value("${aituan.map.amap.regeo-url:https://restapi.amap.com/v3/geocode/regeo}") String amapRegeoUrl) {
    this.restClient = org.springframework.web.client.RestClient.create();
    this.objectMapper = objectMapper;
    this.provider = provider == null ? "local" : provider.trim().toLowerCase();
    this.amapKey = amapKey == null ? "" : amapKey.trim();
    this.amapGeocodeUrl = amapGeocodeUrl;
    this.amapDistanceUrl = amapDistanceUrl;
    this.amapRegeoUrl = amapRegeoUrl;
  }

  GeocodeResult geocode(String address) {
    if (!"amap".equals(provider) || !StringUtils.hasText(amapKey) || !StringUtils.hasText(address)) {
      return null;
    }
    try {
      URI uri = URI.create(amapGeocodeUrl + separator(amapGeocodeUrl)
          + "key=" + encode(amapKey)
          + "&address=" + encode(address.trim())
          + "&output=JSON");
      String body = restClient.get().uri(uri).retrieve().body(String.class);
      JsonNode root = objectMapper.readTree(body);
      if (!"1".equals(root.path("status").asText())) {
        return null;
      }
      JsonNode first = root.path("geocodes").isArray() && !root.path("geocodes").isEmpty()
          ? root.path("geocodes").get(0)
          : null;
      if (first == null) {
        return null;
      }
      String[] parts = first.path("location").asText("").split(",");
      if (parts.length != 2) {
        return null;
      }
      return new GeocodeResult(BigDecimal.valueOf(Double.parseDouble(parts[0])), BigDecimal.valueOf(Double.parseDouble(parts[1])));
    } catch (IllegalArgumentException | RestClientException | IOException ex) {
      return null;
    }
  }

  ReverseGeocodeResult reverseGeocode(double longitude, double latitude) {
    ReverseGeocodeResult fallback = new ReverseGeocodeResult(longitude, latitude, "当前位置", "", "", "", "", "");
    if (!"amap".equals(provider) || !StringUtils.hasText(amapKey)) {
      return fallback;
    }
    try {
      URI uri = URI.create(amapRegeoUrl + separator(amapRegeoUrl)
          + "key=" + encode(amapKey)
          + "&location=" + longitude + "," + latitude
          + "&extensions=base&output=JSON");
      String body = restClient.get().uri(uri).retrieve().body(String.class);
      JsonNode root = objectMapper.readTree(body);
      if (!"1".equals(root.path("status").asText())) {
        return fallback;
      }
      JsonNode regeocode = root.path("regeocode");
      JsonNode address = regeocode.path("addressComponent");
      String formatted = regeocode.path("formatted_address").asText("");
      String province = textNode(address.path("province"));
      String city = textNode(address.path("city"));
      if (!StringUtils.hasText(city)) city = province;
      String district = textNode(address.path("district"));
      String township = textNode(address.path("township"));
      String street = textNode(address.path("streetNumber").path("street"));
      String number = textNode(address.path("streetNumber").path("number"));
      String streetText = StringUtils.hasText(number) ? street + number : street;
      String label = firstText(streetText, township, district, formatted, "当前位置");
      return new ReverseGeocodeResult(longitude, latitude, label, formatted, province, city, district, streetText);
    } catch (IllegalArgumentException | RestClientException | IOException ex) {
      return fallback;
    }
  }

  DistanceEstimate estimate(double userLatitude, double userLongitude, double storeLatitude, double storeLongitude) {
    if ("amap".equals(provider) && StringUtils.hasText(amapKey)) {
      DistanceEstimate remote = estimateByAmap(userLatitude, userLongitude, storeLatitude, storeLongitude);
      if (remote != null) return remote;
    }
    return estimateLocally(userLatitude, userLongitude, storeLatitude, storeLongitude);
  }

  DistanceEstimate estimateLocally(double userLatitude, double userLongitude, double storeLatitude, double storeLongitude) {
    double distanceKm = distanceKm(userLatitude, userLongitude, storeLatitude, storeLongitude);
    return new DistanceEstimate(distanceText(distanceKm), localTimeText(distanceKm));
  }

  double distanceKm(double lat1, double lng1, double lat2, double lng2) {
    double earthRadiusKm = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  private DistanceEstimate estimateByAmap(double userLatitude, double userLongitude, double storeLatitude, double storeLongitude) {
    try {
      URI uri = URI.create(amapDistanceUrl + separator(amapDistanceUrl)
          + "origins=" + userLongitude + "," + userLatitude
          + "&destination=" + storeLongitude + "," + storeLatitude
          + "&type=1&key=" + encode(amapKey));
      String body = restClient.get().uri(uri).retrieve().body(String.class);
      JsonNode root = objectMapper.readTree(body);
      if (!"1".equals(root.path("status").asText())) return null;
      JsonNode first = root.path("results").isArray() && !root.path("results").isEmpty() ? root.path("results").get(0) : null;
      if (first == null) return null;
      double meters = first.path("distance").asDouble(0);
      int seconds = first.path("duration").asInt(0);
      if (meters <= 0) return null;
      return new DistanceEstimate(distanceText(meters / 1000), seconds > 0 ? timeText((int) Math.ceil(seconds / 60.0)) : localTimeText(meters / 1000));
    } catch (RestClientException | IOException ex) {
      return null;
    }
  }

  private String distanceText(double distanceKm) {
    if (distanceKm < 1) return Math.max(50, (int) Math.round(distanceKm * 1000 / 10) * 10) + "m";
    return BigDecimal.valueOf(distanceKm).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "km";
  }

  private String localTimeText(double distanceKm) {
    return timeText(Math.max(5, (int) Math.ceil(distanceKm / 4.0 * 60)));
  }

  private String timeText(int minutes) {
    return "约" + Math.max(1, minutes) + "分钟";
  }

  private String separator(String url) {
    return url.contains("?") ? "&" : "?";
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String textNode(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull() || node.isArray()) return "";
    return node.asText("").trim();
  }

  private String firstText(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) return value.trim();
    }
    return "";
  }

  record DistanceEstimate(String distanceText, String estimatedTimeText) {}
  record GeocodeResult(BigDecimal longitude, BigDecimal latitude) {}
  record ReverseGeocodeResult(double longitude, double latitude, String label, String formattedAddress, String province, String city, String district, String street) {}
}

@Service
class InternalServiceClient {
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final String identityUrl;
  private final String tradeUrl;
  private final String platformUrl;
  private final String serviceToken;

  InternalServiceClient(
      RestTemplate restTemplate,
      ObjectMapper objectMapper,
      @Value("${aituan.services.identity-url:http://identity-asset-service:8081}") String identityUrl,
      @Value("${aituan.services.trade-url:http://trade-fulfillment-service:8083}") String tradeUrl,
      @Value("${aituan.services.platform-url:http://engagement-platform-service:8084}") String platformUrl,
      @Value("${aituan.internal.service-token:}") String serviceToken) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.identityUrl = trimRight(identityUrl);
    this.tradeUrl = trimRight(tradeUrl);
    this.platformUrl = trimRight(platformUrl);
    this.serviceToken = serviceToken;
  }

  HomeSummaryView homeSummary(Long userId) {
    if (userId == null) return new HomeSummaryView(0);
    HomeSummaryView value = get(identityUrl + "/internal/users/" + userId + "/home-summary", new ParameterizedTypeReference<HomeSummaryView>() {});
    return value == null ? new HomeSummaryView(0) : value;
  }

  List<PreferenceSignalView> preferenceSignals(Long userId) {
    if (userId == null) return List.of();
    List<PreferenceSignalView> value = get(identityUrl + "/internal/users/" + userId + "/preference-signals", new ParameterizedTypeReference<List<PreferenceSignalView>>() {});
    return value == null ? List.of() : value;
  }

  ReviewSummaryView reviewSummary(long storeId) {
    ReviewSummaryView value = get(platformUrl + "/internal/reviews/stores/" + storeId + "/summary", new ParameterizedTypeReference<ReviewSummaryView>() {});
    return value == null ? new ReviewSummaryView(BigDecimal.ZERO, 0, List.of()) : value;
  }

  StoreOrderMetricsView orderMetrics(long storeId) {
    StoreOrderMetricsView value = get(tradeUrl + "/internal/metrics/stores/" + storeId + "/orders", new ParameterizedTypeReference<StoreOrderMetricsView>() {});
    return value == null ? new StoreOrderMetricsView(0, BigDecimal.ZERO, 0) : value;
  }

  StoreEngagementMetricsView engagementMetrics(long storeId) {
    StoreEngagementMetricsView value = get(platformUrl + "/internal/metrics/stores/" + storeId + "/engagement", new ParameterizedTypeReference<StoreEngagementMetricsView>() {});
    return value == null ? new StoreEngagementMetricsView(BigDecimal.ZERO, 0, 0, 0) : value;
  }

  ServiceCommandResult provisionMerchantAccount(MerchantAccountProvisionRequest request, String idempotencyKey) {
    ServiceCommandResult value = post(identityUrl + "/internal/merchant-accounts/provision", request, idempotencyKey, new ParameterizedTypeReference<ServiceCommandResult>() {});
    return value == null ? new ServiceCommandResult(false, null, "账号服务暂不可用") : value;
  }

  void writeAuditLog(AuditLogRequest request, String idempotencyKey) {
    post(platformUrl + "/internal/audit-logs", request, idempotencyKey, new ParameterizedTypeReference<Object>() {});
  }

  private <T> T get(String url, ParameterizedTypeReference<T> dataType) {
    try {
      ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers(null)), JsonNode.class);
      return data(response.getBody(), dataType);
    } catch (RestClientException | IllegalArgumentException ex) {
      return null;
    }
  }

  private <T> T post(String url, Object request, String idempotencyKey, ParameterizedTypeReference<T> dataType) {
    try {
      ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request, headers(idempotencyKey)), JsonNode.class);
      return data(response.getBody(), dataType);
    } catch (RestClientException | IllegalArgumentException ex) {
      return null;
    }
  }

  private <T> T data(JsonNode root, ParameterizedTypeReference<T> dataType) {
    if (root == null || root.path("code").asInt(-1) != 0 || root.path("data").isMissingNode()) return null;
    return objectMapper.convertValue(root.path("data"), objectMapper.getTypeFactory().constructType(dataType.getType()));
  }

  private HttpHeaders headers(String idempotencyKey) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Caller-Service", "merchant-catalog-service");
    headers.set("X-Request-Id", com.aituan.common.api.RequestIds.current());
    headers.set("X-Service-Token", serviceToken);
    if (StringUtils.hasText(idempotencyKey)) {
      headers.set("Idempotency-Key", idempotencyKey);
    }
    return headers;
  }

  private String trimRight(String value) {
    if (value == null || value.isBlank()) return "";
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
