package com.aituan.identity.internal;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class InternalServiceGuard {
  private static final Set<String> ALLOWED_CALLERS = Set.of(
      "merchant-catalog-service", "trade-fulfillment-service", "engagement-platform-service");

  private final String serviceToken;

  InternalServiceGuard(@Value("${aituan.internal.service-token:}") String serviceToken) {
    this.serviceToken = serviceToken == null ? "" : serviceToken;
  }

  void require(String requestId, String callerService, String providedToken) {
    if (requestId == null || requestId.isBlank()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "内部接口必须提供 X-Request-Id");
    }
    if (callerService == null || callerService.isBlank() || !ALLOWED_CALLERS.contains(callerService)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "内部服务调用方无效");
    }
    if (serviceToken.isBlank() || !serviceToken.equals(providedToken)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "内部服务凭证无效");
    }
  }
}
