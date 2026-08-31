package com.aituan.common.security;

import com.aituan.common.enums.AccountType;

public record CurrentUser(Long accountId, Long userId, AccountType accountType, String displayName) {

  public boolean isUser() {
    return accountType == AccountType.USER;
  }
}
