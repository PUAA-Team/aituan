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
  private final AiAssistantRepository assistantRepository;

  AiAgentService(AiChatClient aiChatClient, AiSkillRegistry skillRegistry, AiAssistantRepository assistantRepository) {
    this.aiChatClient = aiChatClient;
    this.skillRegistry = skillRegistry;
    this.assistantRepository = assistantRepository;
  }

  public AiAssistantResponse userAssistant(CurrentUser currentUser, AiAssistantMessageRequest request) {
    String content = clean(request.content());
    AiAssistantRepository.ConversationRow conversation = resolveConversation(currentUser, request.conversationId(), content);
    String memoryText = conversationMemory(conversation.id(), currentUser.userId());
    long userMessageId = assistantRepository.insertMessage(
        conversation.id(), currentUser.userId(), "user", content, List.of(), List.of(), List.of(), List.of(), false);
    assistantRepository.touchConversation(conversation.id(), userMessageId, titleFrom(content));
    AiSkillContext context = new AiSkillContext(currentUser, content, memoryText, conversation.id(), null, null, "user_assistant");
    List<AiSkillResult> skillResults = skillRegistry.evaluate(context);
    List<AiAssistantStep> steps = steps(skillResults, true);
    AgentReply reply = generateReply("用户端助手", content, skillResults, fallbackReply(content, skillResults));
    List<AiAssistantCard> cards = cards(skillResults);
    List<AiAssistantAction> actions = actions(skillResults);
    List<String> usedSkills = skillResults.stream().map(AiSkillResult::name).toList();
    long assistantMessageId = assistantRepository.insertMessage(
        conversation.id(), currentUser.userId(), "assistant", reply.content(), cards, actions, steps, usedSkills, reply.modelUsed());
    assistantRepository.touchConversation(conversation.id(), assistantMessageId, titleFrom(content));
    return new AiAssistantResponse(
        conversation.conversationNo(),
        reply.content(),
        cards,
        actions,
        steps,
        usedSkills,
        reply.modelUsed());
  }

  public AiAssistantHistoryResponse currentConversation(CurrentUser currentUser) {
    return assistantRepository.findCurrentConversation(currentUser.userId())
        .map(row -> history(currentUser, row))
        .orElseGet(() -> new AiAssistantHistoryResponse(null, List.of()));
  }

  public AiAssistantHistoryResponse conversation(CurrentUser currentUser, String conversationNo) {
    return assistantRepository.findConversation(currentUser.userId(), conversationNo)
        .map(row -> history(currentUser, row))
        .orElseGet(() -> new AiAssistantHistoryResponse(null, List.of()));
  }

  public String platformSupportReply(CurrentUser currentUser, Long sessionId, Long relatedOrderId, String content) {
    String cleaned = clean(content);
    AiSkillContext context = new AiSkillContext(currentUser, cleaned, "", null, sessionId, relatedOrderId, "platform_support");
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

  private AiAssistantRepository.ConversationRow resolveConversation(CurrentUser currentUser, String conversationNo, String content) {
    if (conversationNo != null && !conversationNo.isBlank()) {
      return assistantRepository.findConversation(currentUser.userId(), conversationNo.trim())
          .orElseGet(() -> assistantRepository.createConversation(currentUser.userId(), conversationNo.trim(), titleFrom(content)));
    }
    return assistantRepository.findCurrentConversation(currentUser.userId())
        .orElseGet(() -> assistantRepository.createConversation(
            currentUser.userId(), UUID.randomUUID().toString(), titleFrom(content)));
  }

  private AiAssistantHistoryResponse history(CurrentUser currentUser, AiAssistantRepository.ConversationRow conversation) {
    List<AiAssistantMessageView> messages = assistantRepository.listMessages(conversation.id(), currentUser.userId(), 80)
        .stream()
        .map(row -> new AiAssistantMessageView(
            row.role(),
            row.content(),
            row.cards(),
            row.actions(),
            row.steps(),
            row.usedSkills(),
            row.modelUsed(),
            row.createdAt() == null ? null : row.createdAt().toString()))
        .toList();
    return new AiAssistantHistoryResponse(conversation.conversationNo(), messages);
  }

  private String conversationMemory(long conversationId, long userId) {
    List<AiAssistantRepository.MessageRow> messages = assistantRepository.listMessages(conversationId, userId, 12);
    if (messages.isEmpty()) return "";
    StringBuilder builder = new StringBuilder();
    int start = Math.max(0, messages.size() - 8);
    for (int i = start; i < messages.size(); i++) {
      AiAssistantRepository.MessageRow row = messages.get(i);
      builder.append(row.role()).append(": ").append(limit(row.content(), 220)).append("\n");
      if (!row.cards().isEmpty()) {
        builder.append("cards: ");
        for (AiAssistantCard card : row.cards()) {
          builder.append(card.type()).append("(").append(card.title()).append(", ").append(card.payload()).append("); ");
        }
        builder.append("\n");
      }
    }
    return builder.toString();
  }

  private List<AiAssistantStep> steps(List<AiSkillResult> skills, boolean replyStep) {
    List<AiAssistantStep> steps = new ArrayList<>();
    steps.add(new AiAssistantStep("识别问题类型", "根据消息内容选择可用业务信息", "done"));
    for (AiSkillResult skill : skills) {
      steps.add(new AiAssistantStep(stepTitle(skill.name()), skill.title(), "done"));
    }
    steps.add(new AiAssistantStep(replyStep ? "整理回复建议" : "准备回复", "结合已查询信息生成可执行建议", "done"));
    return steps;
  }

  private String stepTitle(String skillName) {
    return switch (skillName) {
      case "order_lookup" -> "调用了订单信息";
      case "coupon_lookup" -> "调用了优惠券信息";
      case "governance_entry" -> "调用了治理入口信息";
      case "store_lookup" -> "调用了店铺信息";
      case "item_lookup" -> "调用了商品服务信息";
      case "review_lookup" -> "调用了评价信息";
      case "complaint_lookup" -> "调用了投诉工单信息";
      case "favorite_lookup" -> "调用了收藏信息";
      case "message_lookup" -> "调用了站内消息";
      case "address_lookup" -> "调用了地址信息";
      case "cart_lookup" -> "调用了购物车信息";
      case "member_lookup" -> "调用了会员信息";
      case "support_lookup" -> "调用了客服会话信息";
      case "account_summary" -> "调用了账号摘要";
      default -> "调用了" + skillName;
    };
  }

  private String titleFrom(String content) {
    String cleaned = clean(content).replaceAll("\\s+", " ");
    if (cleaned.isBlank()) return "新对话";
    return limit(cleaned, 30);
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
