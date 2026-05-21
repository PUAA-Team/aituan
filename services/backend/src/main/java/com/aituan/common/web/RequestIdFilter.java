package com.aituan.common.web;

import com.aituan.common.api.RequestIds;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String requestId = request.getHeader("X-Request-Id");
    if (requestId == null || requestId.isBlank()) {
      requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
    }
    RequestIds.set(requestId);
    response.setHeader("X-Request-Id", requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      RequestIds.clear();
    }
  }
}
