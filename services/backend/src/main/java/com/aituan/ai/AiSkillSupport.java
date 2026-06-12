package com.aituan.ai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AiSkillSupport {
  private static final List<String> BROAD_USER_INFO_WORDS = List.of(
      "全部信息", "所有信息", "全面", "全量", "所有业务", "账号总览", "个人总览", "我的情况", "个人情况", "账号情况", "我的信息", "总结一下");
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

  static List<String> businessTypes(String text) {
    LinkedHashSet<String> types = new LinkedHashSet<>();
    String value = text == null ? "" : text.trim();
    if (containsAny(value, "group_buy", "团购", "到店套餐", "多人餐", "双人餐", "核销套餐")) {
      types.add("group_buy");
      return new ArrayList<>(types);
    }
    if (containsAny(value, "takeaway", "外卖", "吃饭", "吃点", "吃什么", "汉堡", "拌饭", "炸鸡", "烤肉", "小馆", "美食", "餐厅")) {
      types.add("takeaway");
    }
    if (containsAny(value, "hotel", "酒店", "住宿", "民宿", "房间")) {
      types.add("hotel");
    }
    if (containsAny(value, "movie", "电影", "影院", "影城", "剧场", "演出")) {
      types.add("movie");
    }
    if (containsAny(value, "massage", "按摩", "足疗", "足道", "洗脚", "肩颈")) {
      types.add("massage");
    }
    if (containsAny(value, "ticket", "景点", "门票", "观景", "游玩", "亲子")) {
      types.add("ticket");
    }
    if (containsAny(value, "entertainment", "休闲", "玩乐", "娱乐", "密室", "电玩城", "VR", "vr", "朋友聚会")) {
      types.add("entertainment");
    }
    if (containsAny(value, "beauty", "SPA", "spa", "丽人", "美容", "医美", "护理", "放松")) {
      types.add("beauty");
    }
    if (containsAny(value, "休闲玩乐", "玩乐", "休闲娱乐", "附近玩", "周边玩")) {
      types.add("movie");
      types.add("ticket");
      types.add("massage");
      types.add("entertainment");
    }
    return new ArrayList<>(types);
  }

  static String businessTypeHint(String rawBusinessType) {
    if (rawBusinessType == null || rawBusinessType.isBlank()) return "";
    LinkedHashSet<String> words = new LinkedHashSet<>();
    for (String value : rawBusinessType.split("[,，\\s]+")) {
      switch (value.trim().toLowerCase()) {
        case "group_buy" -> words.addAll(List.of("团购", "到店套餐", "核销套餐"));
        case "takeaway" -> words.addAll(List.of("外卖", "美食", "吃"));
        case "hotel" -> words.addAll(List.of("酒店", "住宿"));
        case "movie" -> words.addAll(List.of("电影", "影院", "剧场"));
        case "beauty" -> words.addAll(List.of("丽人", "美容", "医美", "SPA", "护理"));
        case "ticket" -> words.addAll(List.of("景点", "门票", "观景", "游玩"));
        case "massage" -> words.addAll(List.of("按摩", "足疗", "足道", "肩颈"));
        case "entertainment" -> words.addAll(List.of("休闲", "玩乐", "娱乐", "密室", "电玩城"));
        default -> {
          if (!value.isBlank()) words.add(value.trim());
        }
      }
    }
    return String.join(" ", words);
  }

  static String businessTypeLabel(List<String> types) {
    if (types == null || types.isEmpty()) return "开放";
    Set<String> typeSet = new LinkedHashSet<>(types);
    if (typeSet.equals(Set.of("group_buy"))) return "团购";
    if (typeSet.containsAll(List.of("movie", "ticket", "massage", "entertainment"))
        && typeSet.size() <= 5) return "休闲玩乐";
    if (typeSet.equals(Set.of("takeaway"))) return "美食外卖";
    if (typeSet.equals(Set.of("hotel"))) return "酒店住宿";
    if (typeSet.equals(Set.of("movie"))) return "电影演出";
    if (typeSet.equals(Set.of("massage"))) return "按摩足疗";
    if (typeSet.equals(Set.of("ticket"))) return "景点门票";
    if (typeSet.equals(Set.of("entertainment"))) return "休闲娱乐";
    if (typeSet.equals(Set.of("beauty"))) return "丽人护理";
    return "匹配业务";
  }

  static String searchKeyword(String key, List<String> types) {
    if (key != null && !key.isBlank()) return key;
    return switch (businessTypeLabel(types)) {
      case "团购" -> "团购";
      case "休闲玩乐" -> "休闲玩乐";
      case "美食外卖" -> "美食";
      case "酒店住宿" -> "酒店";
      case "电影演出" -> "电影";
      case "按摩足疗" -> "足疗";
      case "景点门票" -> "门票";
      case "休闲娱乐" -> "休闲娱乐";
      case "丽人护理" -> "SPA";
      default -> "";
    };
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
