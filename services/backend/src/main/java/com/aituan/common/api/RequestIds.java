package com.aituan.common.api;

import java.util.Optional;
import org.slf4j.MDC;

public final class RequestIds {
  private static final String KEY = "requestId";

  private RequestIds() {}

  public static String current() {
    return Optional.ofNullable(MDC.get(KEY)).orElse("req-local");
  }

  public static void set(String requestId) {
    MDC.put(KEY, requestId);
  }

  public static void clear() {
    MDC.remove(KEY);
  }
}
