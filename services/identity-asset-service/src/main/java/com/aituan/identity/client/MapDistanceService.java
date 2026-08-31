package com.aituan.identity.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class MapDistanceService {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String provider;
  private final String amapKey;
  private final String amapGeocodeUrl;

  public MapDistanceService(
      ObjectMapper objectMapper,
      @Value("${aituan.map.provider:local}") String provider,
      @Value("${aituan.map.amap.web-api-key:}") String amapKey,
      @Value("${aituan.map.amap.geocode-url:https://restapi.amap.com/v3/geocode/geo}") String amapGeocodeUrl) {
    this.restClient = RestClient.create();
    this.objectMapper = objectMapper;
    this.provider = provider == null ? "local" : provider.trim().toLowerCase();
    this.amapKey = amapKey == null ? "" : amapKey.trim();
    this.amapGeocodeUrl = amapGeocodeUrl;
  }

  public GeocodeResult geocode(String address) {
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
      String location = first.path("location").asText("");
      String[] parts = location.split(",");
      if (parts.length != 2) {
        return null;
      }
      return new GeocodeResult(
          BigDecimal.valueOf(Double.parseDouble(parts[0])),
          BigDecimal.valueOf(Double.parseDouble(parts[1])));
    } catch (IllegalArgumentException | RestClientException | IOException ex) {
      return null;
    }
  }

  private String separator(String url) {
    return url.contains("?") ? "&" : "?";
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  public record GeocodeResult(BigDecimal longitude, BigDecimal latitude) {}
}
