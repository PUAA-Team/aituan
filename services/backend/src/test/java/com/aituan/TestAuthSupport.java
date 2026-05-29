package com.aituan;

import com.aituan.common.enums.AccountType;
import com.aituan.common.security.CurrentUser;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 测试用：在 SecurityContext 中注入 CurrentUser，覆盖鉴权。 */
public final class TestAuthSupport {
  private TestAuthSupport() {}

  public static void loginAsUser(long accountId, long userId) {
    set(new CurrentUser(accountId, userId, AccountType.USER, "test-user"));
  }

  public static void loginAsMerchant(long accountId) {
    set(new CurrentUser(accountId, null, AccountType.MERCHANT, "test-merchant"));
  }

  public static void loginAsAdmin(long accountId) {
    set(new CurrentUser(accountId, null, AccountType.ADMIN, "test-admin"));
  }

  public static void clear() {
    SecurityContextHolder.clearContext();
  }

  private static void set(CurrentUser user) {
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(user, "n/a", List.of());
    SecurityContextHolder.getContext().setAuthentication(token);
  }
}
