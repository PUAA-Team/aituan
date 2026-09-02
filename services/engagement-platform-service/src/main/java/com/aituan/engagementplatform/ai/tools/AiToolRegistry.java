package com.aituan.engagementplatform.ai;

import com.aituan.common.security.CurrentUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class AiToolRegistry {
  private final Map<String, AiSkill> skills = new LinkedHashMap<>();
  private final List<AiToolDefinition> definitions;

  AiToolRegistry(List<AiSkill> skillList) {
    for (AiSkill skill : skillList) {
      skills.put(skill.name(), skill);
    }
    definitions = List.of(
        definition("review_lookup", "查询当前用户真实评价、评分、点赞和举报入口。适合评价、评分、评论、举报评价问题。"),
        definition("complaint_lookup", "查询当前用户投诉工单状态、处理进度和证据说明。适合投诉、纠纷、平台处理进度。"),
        definition("governance_entry", "提供投诉、举报、平台客服和转人工入口。适合风险、纠纷、食品安全、服务态度、人工客服诉求。"),
        definition("support_lookup", "查询当前用户客服会话、平台介入、人工状态和关联订单。适合客服、平台客服、商家客服、会话进度。"));
  }

  List<AiToolDefinition> definitions() {
    return definitions;
  }

  Optional<AiSkillResult> execute(
      AiToolCall call,
      CurrentUser currentUser,
      String originalMessage,
      String memoryText,
      Long conversationId,
      Long supportSessionId,
      Long relatedOrderId,
      String channel) {
    AiSkill skill = skills.get(call.name());
    if (skill == null) return Optional.empty();
    String forcedMessage = forcedMessage(call, originalMessage);
    AiSkillContext context = new AiSkillContext(
        currentUser,
        forcedMessage,
        memoryText,
        conversationId,
        supportSessionId,
        relatedOrderId,
        channel);
    return skill.evaluate(context);
  }

  List<AiToolCall> fallbackCalls(String message, String memoryText, String channel) {
    String current = message == null ? "" : message;
    String memory = memoryText == null ? "" : memoryText;
    boolean followUp = containsAny(current, "刚才", "上面", "前面", "第一个", "第二个", "第三个", "这个", "那个", "继续", "详情");
    List<String> names = new ArrayList<>();
    if (containsAny(current, "全部信息", "所有信息", "全面", "总览", "我的情况")) {
      names.addAll(List.of("support_lookup", "review_lookup", "complaint_lookup", "governance_entry"));
    } else {
      if (containsAny(current, "评价", "评分", "评论", "点赞", "举报评价")) names.add("review_lookup");
      if (containsAny(current, "投诉", "举报", "纠纷", "食品安全", "服务态度")) names.add("complaint_lookup");
      if (containsAny(current, "客服", "人工", "平台介入", "商家客服", "平台客服")) names.add("support_lookup");
      if (containsAny(current, "投诉", "举报", "纠纷", "转人工", "人工客服", "食品安全")) names.add("governance_entry");
      if (followUp && containsAny(memory, "reviewId", "评价")) names.add("review_lookup");
      if (followUp && containsAny(memory, "工单", "投诉")) names.add("complaint_lookup");
    }
    if ("platform_support".equals(channel) && names.isEmpty()) {
      names.addAll(List.of("support_lookup", "governance_entry"));
    }
    if (names.isEmpty()) {
      names.add("support_lookup");
      names.add("governance_entry");
    }
    return names.stream().distinct().map(name -> new AiToolCall("local-" + name, name, Map.of("query", current))).toList();
  }

  private AiToolDefinition definition(String name, String description) {
    return new AiToolDefinition(name, description, Map.of(
        "type", "object",
        "properties", Map.of(
            "query", Map.of("type", "string", "description", "用户原始查询或提炼后的搜索词"),
            "businessType", Map.of("type", "string", "description", "可选业务类型，如 group_buy、takeaway、hotel、movie、beauty、ticket、massage、entertainment"),
            "limit", Map.of("type", "integer", "description", "最多返回数量")),
        "required", List.of("query")));
  }

  private String forcedMessage(AiToolCall call, String originalMessage) {
    String query = textArg(call.arguments(), "query");
    String businessType = textArg(call.arguments(), "businessType");
    String base = (query.isBlank() ? originalMessage : query) == null ? "" : (query.isBlank() ? originalMessage : query);
    String typeHint = AiSkillSupport.businessTypeHint(businessType);
    typeHint = typeHint.isBlank() ? "" : " " + typeHint + " ";
    return switch (call.name()) {
      case "account_summary" -> "账号 全部信息 账号总览 " + base;
      case "order_lookup" -> "订单 退款 配送 券码 预约 " + base;
      case "coupon_lookup" -> "优惠 优惠券 红包 满减 领券 " + base;
      case "store_lookup" -> "店 商家 门店 附近 推荐 周边 " + typeHint + base;
      case "item_lookup" -> "商品 套餐 服务 价格 库存 " + typeHint + base;
      case "review_lookup" -> "评价 评分 评论 举报评价 " + base;
      case "complaint_lookup" -> "投诉 工单 纠纷 举报 " + base;
      case "governance_entry" -> "投诉 举报 纠纷 转人工 人工客服 " + base;
      case "support_lookup" -> "客服 平台客服 商家客服 人工 平台介入 " + base;
      case "favorite_lookup" -> "收藏 喜欢 想去 关注 " + base;
      case "address_lookup" -> "地址 收货 配送地址 电话 送到哪 " + base;
      case "cart_lookup" -> "购物车 加购 结算 买哪些 " + base;
      case "message_lookup" -> "消息 通知 站内信 未读 " + base;
      case "member_lookup" -> "会员 等级 成长值 权益 " + base;
      default -> base;
    };
  }

  private String textArg(Map<String, Object> arguments, String key) {
    Object value = arguments == null ? null : arguments.get(key);
    return value == null ? "" : value.toString().trim();
  }

  private boolean containsAny(String text, String... words) {
    String value = text == null ? "" : text;
    for (String word : words) {
      if (value.contains(word)) return true;
    }
    return false;
  }
}
