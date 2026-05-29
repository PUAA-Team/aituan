package com.aituan.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private final String amapDistanceUrl;

  public MapDistanceService(
      ObjectMapper objectMapper,
      @Value("${aituan.map.provider:local}") String provider,
      @Value("${aituan.map.amap.web-api-key:}") String amapKey,
      @Value("${aituan.map.amap.geocode-url:https://restapi.amap.com/v3/geocode/geo}") String amapGeocodeUrl,
      @Value("${aituan.map.amap.distance-url:https://restapi.amap.com/v3/distance}") String amapDistanceUrl) {
    this.restClient = RestClient.create();
    this.objectMapper = objectMapper;
    this.provider = provider == null ? "local" : provider.trim().toLowerCase();
    this.amapKey = amapKey == null ? "" : amapKey.trim();
    this.amapGeocodeUrl = amapGeocodeUrl;
    this.amapDistanceUrl = amapDistanceUrl;
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

  public DistanceEstimate estimate(double userLatitude, double userLongitude, double storeLatitude, double storeLongitude) {
    if ("amap".equals(provider) && StringUtils.hasText(amapKey)) {
      DistanceEstimate remote = estimateByAmap(userLatitude, userLongitude, storeLatitude, storeLongitude);
      if (remote != null) {
        return remote;
      }
    }
    return estimateLocally(userLatitude, userLongitude, storeLatitude, storeLongitude);
  }

  public DistanceEstimate estimateLocally(double userLatitude, double userLongitude, double storeLatitude, double storeLongitude) {
    double distanceKm = distanceKm(userLatitude, userLongitude, storeLatitude, storeLongitude);
    return new DistanceEstimate(distanceText(distanceKm), localTimeText(distanceKm));
  }

  public double distanceKm(double lat1, double lng1, double lat2, double lng2) {
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
      if (!"1".equals(root.path("status").asText())) {
        return null;
      }
      JsonNode first = root.path("results").isArray() && !root.path("results").isEmpty()
          ? root.path("results").get(0)
          : null;
      if (first == null) {
        return null;
      }
      double meters = first.path("distance").asDouble(0);
      int seconds = first.path("duration").asInt(0);
      if (meters <= 0) {
        return null;
      }
      return new DistanceEstimate(distanceText(meters / 1000), seconds > 0 ? timeText((int) Math.ceil(seconds / 60.0)) : localTimeText(meters / 1000));
    } catch (RestClientException | IOException ex) {
      return null;
    }
  }

  private String distanceText(double distanceKm) {
    if (distanceKm < 1) {
      return Math.max(50, (int) Math.round(distanceKm * 1000 / 10) * 10) + "m";
    }
    return BigDecimal.valueOf(distanceKm).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "km";
  }

  private String localTimeText(double distanceKm) {
    int minutes = Math.max(5, (int) Math.ceil(distanceKm / 4.0 * 60));
    return timeText(minutes);
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

  public record DistanceEstimate(String distanceText, String estimatedTimeText) {}

  public record GeocodeResult(BigDecimal longitude, BigDecimal latitude) {}
}
