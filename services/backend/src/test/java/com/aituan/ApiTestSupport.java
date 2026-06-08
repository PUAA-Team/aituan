package com.aituan;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.JwtTokenService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** API 集成测试工具：生成真实 JWT，并统一断言响应壳。 */
public final class ApiTestSupport {
  private ApiTestSupport() {}

  public static final MediaType JSON = MediaType.APPLICATION_JSON;

  public static String userToken(JwtTokenService jwtTokenService) {
    return token(jwtTokenService, 1L, 1L, AccountType.USER, "爱团用户");
  }

  public static String merchantToken(JwtTokenService jwtTokenService) {
    return token(jwtTokenService, 2L, 2L, AccountType.MERCHANT, "demo_merchant");
  }

  public static String adminToken(JwtTokenService jwtTokenService) {
    return token(jwtTokenService, 3L, 3L, AccountType.ADMIN, "demo_admin");
  }

  public static MockHttpServletRequestBuilder bearer(
      MockHttpServletRequestBuilder request, String token) {
    return request.header("Authorization", "Bearer " + token);
  }

  public static ResultMatcher okResponse() {
    return jsonPath("$.code").value(0);
  }

  public static ResultMatcher businessError(ErrorCode errorCode) {
    return jsonPath("$.code").value(errorCode.code());
  }

  public static ResultMatcher unauthorized() {
    return all(status().isUnauthorized(), jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.code()));
  }

  public static ResultMatcher forbidden() {
    return all(status().isForbidden(), jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));
  }

  public static ResultMatcher badRequest() {
    return all(status().isBadRequest(), jsonPath("$.code").value(ErrorCode.BAD_REQUEST.code()));
  }

  private static ResultMatcher all(ResultMatcher... matchers) {
    return result -> {
      for (ResultMatcher matcher : matchers) {
        matcher.match(result);
      }
    };
  }

  private static String token(
      JwtTokenService jwtTokenService,
      Long accountId,
      Long userId,
      AccountType accountType,
      String displayName) {
    return jwtTokenService.createToken(new CurrentUser(accountId, userId, accountType, displayName));
  }
}
