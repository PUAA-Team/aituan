package com.aituan.merchantcatalog.config;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InternalApiConfig implements WebMvcConfigurer {
  private final InternalServiceAuthInterceptor internalServiceAuthInterceptor;

  public InternalApiConfig(InternalServiceAuthInterceptor internalServiceAuthInterceptor) {
    this.internalServiceAuthInterceptor = internalServiceAuthInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(internalServiceAuthInterceptor).addPathPatterns("/internal/**");
  }
}

@Configuration
class InternalServiceAuthInterceptor implements HandlerInterceptor {
  private final ObjectMapper objectMapper;
  private final String serviceToken;

  InternalServiceAuthInterceptor(
      ObjectMapper objectMapper,
      @Value("${aituan.internal.service-token:dev-internal-token}") String serviceToken) {
    this.objectMapper = objectMapper;
    this.serviceToken = serviceToken;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
    String callerService = request.getHeader("X-Caller-Service");
    String requestId = request.getHeader("X-Request-Id");
    String token = request.getHeader("X-Service-Token");
    if (!StringUtils.hasText(callerService) || !StringUtils.hasText(requestId) || !StringUtils.hasText(token) || !token.equals(serviceToken)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setCharacterEncoding("UTF-8");
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.FORBIDDEN.code(), "内部接口调用头不完整或服务凭证无效")));
      return false;
    }
    return true;
  }
}
