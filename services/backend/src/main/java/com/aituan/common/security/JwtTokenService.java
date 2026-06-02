package com.aituan.common.security;

import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final ObjectMapper objectMapper;
  private final byte[] secret;
  private final long ttlSeconds;

  public JwtTokenService(
      ObjectMapper objectMapper,
      @Value("${aituan.security.jwt-secret:aituan-demo-secret-change-me}") String secret,
      @Value("${aituan.security.jwt-ttl-seconds:604800}") long ttlSeconds) {
    this.objectMapper = objectMapper;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.ttlSeconds = ttlSeconds;
  }

  public String createToken(CurrentUser currentUser) {
    try {
      Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
      Map<String, Object> payload = Map.of(
          "accountId", currentUser.accountId(),
          "userId", currentUser.userId(),
          "accountType", currentUser.accountType().name(),
          "displayName", currentUser.displayName(),
          "exp", Instant.now().plusSeconds(ttlSeconds).getEpochSecond());
      String unsigned = encode(header) + "." + encode(payload);
      return unsigned + "." + sign(unsigned);
    } catch (Exception exception) {
      throw new IllegalStateException("Token 生成失败", exception);
    }
  }

  public CurrentUser parse(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3 || !sign(parts[0] + "." + parts[1]).equals(parts[2])) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }
      Map<?, ?> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), Map.class);
      long exp = ((Number) payload.get("exp")).longValue();
      if (Instant.now().getEpochSecond() > exp) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }
      Long accountId = ((Number) payload.get("accountId")).longValue();
      Long userId = ((Number) payload.get("userId")).longValue();
      AccountType accountType = AccountType.valueOf((String) payload.get("accountType"));
      String displayName = (String) payload.get("displayName");
      return new CurrentUser(accountId, userId, accountType, displayName);
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }

  public long ttlSeconds() {
    return ttlSeconds;
  }

  private String encode(Object value) throws Exception {
    return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
  }

  private String sign(String value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  }
}
