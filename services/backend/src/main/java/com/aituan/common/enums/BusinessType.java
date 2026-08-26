package com.aituan.common.enums;

public enum BusinessType {
  TAKEAWAY("takeaway", "外卖"),
  GROUP_BUY("group_buy", "团购"),
  HOTEL("hotel", "酒店"),
  ENTERTAINMENT("entertainment", "休闲娱乐"),
  MOVIE("movie", "电影演出"),
  BEAUTY("beauty", "丽人医美"),
  TICKET("ticket", "景点门票"),
  MASSAGE("massage", "洗脚");

  private final String code;
  private final String label;

  BusinessType(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }

  public static BusinessType fromCode(String code) {
    for (BusinessType value : values()) {
      if (value.code.equalsIgnoreCase(code)) {
        return value;
      }
    }
    throw new IllegalArgumentException("未知业务类型: " + code);
  }
}
