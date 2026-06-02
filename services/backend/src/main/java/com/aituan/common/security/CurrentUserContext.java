package com.aituan.common.security;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserContext {
  private CurrentUserContext() {}

  public static CurrentUser required() {
    return optional().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
  }

  public static Optional<CurrentUser> optional() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
      return Optional.empty();
    }
    return Optional.of(currentUser);
  }
}
