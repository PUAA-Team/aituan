package com.aituan.common.enums;

public enum DisplayOrderStatus {
  UNPAID("unpaid", "未支付"),
  PENDING("pending", "待完成"),
  UNUSED("unused", "未使用"),
  USED("used", "已使用");

  private final String code;
  private final String label;

  DisplayOrderStatus(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }

  public static DisplayOrderStatus fromCode(String code) {
    for (DisplayOrderStatus value : values()) {
      if (value.code.equalsIgnoreCase(code)) {
        return value;
      }
    }
    throw new IllegalArgumentException("未知订单状态: " + code);
  }
}
