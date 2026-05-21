package com.aituan.common.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(int code, String message, T data, OffsetDateTime timestamp, String requestId) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "success", data, OffsetDateTime.now(), RequestIds.current());
  }

  public static <T> ApiResponse<T> fail(int code, String message) {
    return new ApiResponse<>(code, message, null, OffsetDateTime.now(), RequestIds.current());
  }
}
