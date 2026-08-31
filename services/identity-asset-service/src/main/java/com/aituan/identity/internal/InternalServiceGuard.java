package com.aituan.identity.internal;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class InternalServiceGuard {
  private final String serviceToken;

  InternalServiceGuard(@Value("${aituan.internal.service-token:}") String serviceToken) {
    this.serviceToken = serviceToken == null ? "" : serviceToken;
  }

  void require(String requestId, String callerService, String providedToken) {
    if (requestId == null || requestId.isBlank()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "内部接口必须提供 X-Request-Id");
    }
    if (callerService == null || callerService.isBlank()) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "内部接口必须提供 X-Caller-Service");
    }
    if (!serviceToken.isBlank() && !serviceToken.equals(providedToken)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "内部服务凭证无效");
    }
  }
}
