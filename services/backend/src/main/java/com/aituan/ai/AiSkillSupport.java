package com.aituan.ai;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AiSkillSupport {
  private static final List<String> BROAD_USER_INFO_WORDS = List.of(
      "全部信息", "所有信息", "全面", "帮我看看", "我的情况", "个人情况", "账号情况", "我有什么", "我的信息", "总结一下");
  private static final List<String> FOLLOW_UP_WORDS = List.of(
      "这个", "那个", "刚才", "上面", "前面", "第一", "第二", "第三", "它", "这些", "继续", "详情");
  private static final List<String> DOMAIN_WORDS = List.of(
      "账号", "个人", "资料", "地址", "收货", "购物车", "订单", "配送", "退款", "预约", "券码",
      "优惠券", "红包", "会员", "成长值", "消息", "通知", "收藏", "店", "商家", "门店",
      "商品", "套餐", "服务", "评价", "评分", "评论", "投诉", "工单", "客服", "人工");

  private AiSkillSupport() {}

  static boolean shouldRun(AiSkillContext context, List<String> domainWords) {
    String current = context.normalizedMessage();
    if (containsAny(current, domainWords)) return true;
    if (isBroadUserInfo(current)) return true;
    return isFollowUp(current)
        && !containsAny(current, DOMAIN_WORDS)
        && containsAny(context.memoryText(), domainWords);
  }

  static boolean isBroadUserInfo(String text) {
    return containsAny(text, BROAD_USER_INFO_WORDS);
  }

  static boolean isFollowUp(String text) {
    return containsAny(text, FOLLOW_UP_WORDS);
  }

  static boolean containsAny(String text, List<String> keywords) {
    if (text == null || text.isBlank()) return false;
    for (String keyword : keywords) {
      if (keyword != null && !keyword.isBlank() && text.contains(keyword)) return true;
    }
    return false;
  }

  static boolean containsAny(String text, String... keywords) {
    return containsAny(text, List.of(keywords));
  }

  static String keyword(String text, List<String> candidates) {
    if (text == null) return "";
    for (String candidate : candidates) {
      if (text.contains(candidate)) return candidate;
    }
    String cleaned = text
        .replaceAll("[，。！？、,.!?；;：:]", " ")
        .replaceAll("\\s+", " ")
        .trim();
    if (cleaned.length() > 18) return "";
    return cleaned;
  }

  static String like(String keyword) {
    return keyword == null || keyword.isBlank() ? "" : "%" + keyword + "%";
  }

  static String limit(String value, int max) {
    if (value == null) return "";
    return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
  }

  static String money(BigDecimal value) {
    return value == null ? "0" : value.stripTrailingZeros().toPlainString();
  }

  static String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) return "未绑定";
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }

  static Map<String, Object> params(Object... keyValues) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
    }
    return map;
  }
}
