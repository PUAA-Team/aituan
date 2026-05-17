package com.aituan.common.api;

import java.util.List;

public record PageResponse<T>(List<T> list, int page, int pageSize, long total, boolean hasNext) {

  public static <T> PageResponse<T> of(List<T> list, int page, int pageSize, long total) {
    return new PageResponse<>(list, page, pageSize, total, page * (long) pageSize < total);
  }
}
