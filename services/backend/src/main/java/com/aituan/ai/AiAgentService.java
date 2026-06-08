package com.aituan.ai;

import com.aituan.common.security.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiAgentService {
  private static final String SYSTEM_PROMPT = """
      你是“爱团”平台内的 AI 助手，只回答与本项目用户消费、订单、优惠券、评价、投诉、客服有关的问题。
      规则：
      1. 使用中文，语气专业、简洁、可执行。
      2. skill 上下文是系统查询到的真实业务信息，必须优先使用；不要编造订单、优惠券或处理结果。
      3. 遇到退款、投诉、食品安全、账号纠纷、评价举报等问题，要提示可提交工单或转人工。
      4. 不承诺已经退款、已经处罚商家、已经赔付；只能说明下一步入口和需要补充的材料。
      5. 回复控制在 180 字以内。
      """;

  private final AiChatClient aiChatClient;
  private final AiSkillRegistry skillRegistry;

  AiAgentService(AiChatClient aiChatClient, AiSkillRegistry skillRegistry) {
    this.aiChatClient = aiChatClient;
    this.skillRegistry = skillRegistry;
  }

  public AiAssistantResponse userAssistant(CurrentUser currentUser, AiAssistantMessageRequest request) {
    String content = clean(request.content());
    AiSkillContext context = new AiSkillContext(currentUser, content, null, null, "user_assistant");
    List<AiSkillResult> skillResults = skillRegistry.evaluate(context);
    AgentReply reply = generateReply("用户端助手", content, skillResults, fallbackReply(content, skillResults));
    return new AiAssistantResponse(
        request.conversationId() == null || request.conversationId().isBlank()
            ? UUID.randomUUID().toString()
            : request.conversationId(),
        reply.content(),
        cards(skillResults),
        actions(skillResults),
        skillResults.stream().map(AiSkillResult::name).toList(),
        reply.modelUsed());
  }

  public String platformSupportReply(CurrentUser currentUser, Long sessionId, Long relatedOrderId, String content) {
    String cleaned = clean(content);
    AiSkillContext context = new AiSkillContext(currentUser, cleaned, sessionId, relatedOrderId, "platform_support");
    List<AiSkillResult> skillResults = skillRegistry.evaluate(context);
    String fallback = fallbackReply(cleaned, skillResults);
    return generateReply("平台客服 AI", cleaned, skillResults, fallback).content();
  }

  public String localKeywordReply(String content) {
    return fallbackReply(clean(content), List.of());
  }

  private AgentReply generateReply(String role, String content, List<AiSkillResult> skills, String fallback) {
    String skillContext = skillContext(skills);
    List<AiChatClient.AiChatMessage> messages = List.of(
        new AiChatClient.AiChatMessage("system", SYSTEM_PROMPT),
        new AiChatClient.AiChatMessage("user", "当前角色：" + role + "\n用户消息：" + content + "\n可用业务上下文：\n" + skillContext));
    return aiChatClient.chat(messages)
        .map(text -> new AgentReply(text, true))
        .orElseGet(() -> new AgentReply(fallback, false));
  }

  private String fallbackReply(String content, List<AiSkillResult> skills) {
    if (!skills.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      for (AiSkillResult skill : skills) {
        if (!builder.isEmpty()) builder.append("\n");
        builder.append(skill.content());
      }
      builder.append("\n需要进一步处理时，可打开对应入口或发送“转人工”。");
      return limit(builder.toString(), 420);
    }
    if (containsAny(content, "退款", "退单", "取消")) {
      return "我已记录您的退款/取消诉求。请补充订单号、支付时间和期望处理方式；情况复杂时可点击“转人工”。";
    }
    if (containsAny(content, "投诉", "差评", "纠纷", "举报")) {
      return "我已记录您的投诉诉求。您可以通过投诉入口提交工单并上传证据，也可以发送“转人工”让平台客服接入。";
    }
    if (containsAny(content, "配送", "多久", "催", "慢", "骑手")) {
      return "我已收到配送/时效问题。请补充订单号和当前等待时长，我会优先帮您整理给平台；需要人工可发送“转人工”。";
    }
    if (containsAny(content, "发票", "票据", "抬头")) {
      return "发票问题请补充发票抬头、税号和接收邮箱；如订单状态异常，可发送“转人工”继续处理。";
    }
    if (containsAny(content, "优惠券", "红包", "满减", "活动")) {
      return "优惠券或活动问题请补充券名、订单金额和截图，平台会核对使用规则；需要人工可发送“转人工”。";
    }
    return "平台客服助手已收到。请继续补充订单或问题细节；如需人工处理，可点击“转人工”或直接发送“转人工”。";
  }

  private String skillContext(List<AiSkillResult> skills) {
    if (skills.isEmpty()) return "无命中的业务 skill。";
    StringBuilder builder = new StringBuilder();
    for (AiSkillResult skill : skills) {
      builder.append("- ")
          .append(skill.title())
          .append("：")
          .append(skill.content())
          .append("\n");
    }
    return builder.toString();
  }

  private List<AiAssistantCard> cards(List<AiSkillResult> results) {
    List<AiAssistantCard> cards = new ArrayList<>();
    for (AiSkillResult result : results) {
      cards.addAll(result.cards());
    }
    return cards;
  }

  private List<AiAssistantAction> actions(List<AiSkillResult> results) {
    List<AiAssistantAction> actions = new ArrayList<>();
    for (AiSkillResult result : results) {
      actions.addAll(result.actions());
    }
    if (actions.isEmpty()) {
      actions.add(new AiAssistantAction("找平台客服", null, "/support/sessions", java.util.Map.of()));
      actions.add(new AiAssistantAction("提交投诉", null, "/complaint/submit", java.util.Map.of()));
    }
    return actions;
  }

  private String clean(String content) {
    return content == null ? "" : content.trim();
  }

  private String limit(String value, int maxLength) {
    if (value.length() <= maxLength) return value;
    return value.substring(0, maxLength - 1) + "…";
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  record AgentReply(String content, boolean modelUsed) {}
}
