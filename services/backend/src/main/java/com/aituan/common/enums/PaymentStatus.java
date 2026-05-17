package com.aituan.common.enums;

public enum PaymentStatus {
  UNPAID("unpaid"),
  PAYING("paying"),
  PAID("paid"),
  FAILED("failed"),
  TIMEOUT("timeout");

  private final String code;

  PaymentStatus(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static PaymentStatus fromCode(String code) {
    for (PaymentStatus value : values()) {
      if (value.code.equalsIgnoreCase(code)) {
        return value;
      }
    }
    throw new IllegalArgumentException("未知支付状态: " + code);
  }
}
