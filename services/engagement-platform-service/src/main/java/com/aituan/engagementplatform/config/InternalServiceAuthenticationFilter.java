package com.aituan.engagementplatform.config;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {
  private static final Set<String> ALLOWED_CALLERS = Set.of(
      "identity-asset-service", "merchant-catalog-service", "trade-fulfillment-service");

  private final byte[] expectedToken;
  private final ObjectMapper objectMapper;

  InternalServiceAuthenticationFilter(
      @Value("${aituan.security.internal-token}") String expectedToken,
      ObjectMapper objectMapper) {
    this.expectedToken = expectedToken.getBytes(StandardCharsets.UTF_8);
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/internal/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String caller = request.getHeader("X-Caller-Service");
    String token = request.getHeader("X-Internal-Token");
    boolean tokenMatches = token != null && MessageDigest.isEqual(
        expectedToken, token.getBytes(StandardCharsets.UTF_8));
    if (!ALLOWED_CALLERS.contains(caller) || !tokenMatches) {
      response.setStatus(403);
      response.setCharacterEncoding("UTF-8");
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write(objectMapper.writeValueAsString(
          ApiResponse.fail(ErrorCode.FORBIDDEN.code(), "内部服务身份校验失败")));
      return;
    }
    chain.doFilter(request, response);
  }
}
