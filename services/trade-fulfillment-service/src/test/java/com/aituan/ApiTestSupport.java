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
    return jwtTokenService.createToken(new CurrentUser(1L, 1L, AccountType.USER, "爱团用户"));
  }

  public static MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder request, String token) {
    return request.header("Authorization", "Bearer " + token);
  }

  public static ResultMatcher okResponse() {
    return jsonPath("$.code").value(0);
  }

  public static ResultMatcher unauthorized() {
    return result -> {
      status().isUnauthorized().match(result);
      jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.code()).match(result);
    };
  }
}
