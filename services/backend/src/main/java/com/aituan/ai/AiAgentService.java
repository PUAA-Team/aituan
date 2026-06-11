package com.aituan.ai;

import com.aituan.common.security.CurrentUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiAgentService {
  private static final String SYSTEM_PROMPT = """
      你是“爱团”平台内的高级 AI 助手“小爱同学”，目标是像一个能查系统、能记上下文、能给下一步建议的产品内智能助理。
      你可以回答订单、商品、店铺、优惠券、会员、购物车、地址、收藏、消息、评价、投诉、客服工单等项目内问题。
      严格规则：
      1. 必须优先使用 skill 上下文中的真实业务信息；没有查到就明确说“当前没有查到”，不要编造订单、券、处罚、退款或赔付。
      2. 不要机械复述数据库片段；先判断用户真正想解决什么，再把关键事实和下一步操作组织成自然回答。
      3. 如果用户在追问“刚才那个/第一个/继续”等，结合最近对话记忆理解指代。
      4. 遇到退款、投诉、食品安全、账号纠纷、评价举报、平台介入，要说明入口、材料、当前状态，并提示可转人工。
      5. 回答应具体、聪明、有执行性；不要只说“请补充信息”。确实缺关键信息时，最多问 1 个最重要的问题。
      6. 回复控制在 260 字以内。中文回答。
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
    AgentReply reply = generateReply("用户端助手", content, memoryText, skillResults, fallbackReply(content, skillResults));
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
    return generateReply("平台客服 AI", cleaned, "", skillResults, fallback).content();
  }

  public String localKeywordReply(String content) {
    return fallbackReply(clean(content), List.of());
  }

  private AgentReply generateReply(String role, String content, String memoryText, List<AiSkillResult> skills, String fallback) {
    String skillContext = skillContext(skills);
    List<AiChatClient.AiChatMessage> messages = List.of(
        new AiChatClient.AiChatMessage("system", SYSTEM_PROMPT),
        new AiChatClient.AiChatMessage("user", """
            当前角色：%s
            最近对话记忆：
            %s
            用户本轮消息：%s
            已调用 skill 得到的真实业务上下文：
            %s

            请基于这些真实信息给出自然、具体、可执行的回答。不要输出 JSON，不要列内部字段名，不要声称已经完成未发生的业务动作。
            """.formatted(role, memoryText == null || memoryText.isBlank() ? "无" : memoryText, content, skillContext)));
    return aiChatClient.chat(messages)
        .map(text -> new AgentReply(text, true))
        .orElseGet(() -> new AgentReply(fallback, false));
  }

  private String fallbackReply(String content, List<AiSkillResult> skills) {
    if (!skills.isEmpty()) {
      StringBuilder builder = new StringBuilder("我已根据真实业务信息帮你查到：");
      int count = 0;
      for (AiSkillResult skill : skills) {
        if (count >= 4) break;
        builder.append("\n").append(count + 1).append(". ")
            .append(skill.title()).append("：")
            .append(limit(skill.content().replace("\n", "；"), 110));
        count++;
      }
      if (skills.size() > count) {
        builder.append("\n另外还查到了 ").append(skills.size() - count).append(" 类相关信息，可点下方卡片继续查看。");
      }
      builder.append("\n需要我继续细查某一项，直接说“查第一个订单/这个店铺/我的投诉”；需要人工可发送“转人工”。");
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
    Map<String, AiAssistantAction> unique = new LinkedHashMap<>();
    for (AiAssistantAction action : actions) {
      String key = (action.route() == null ? "" : action.route())
          + "|"
          + (action.message() == null ? "" : action.message())
          + "|"
          + action.label();
      unique.putIfAbsent(key, action);
    }
    return new ArrayList<>(unique.values());
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
