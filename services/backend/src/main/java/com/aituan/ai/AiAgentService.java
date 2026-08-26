package com.aituan.ai;

import com.aituan.common.security.CurrentUser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final AiToolRegistry toolRegistry;
  private final AiAssistantRepository assistantRepository;

  AiAgentService(AiChatClient aiChatClient, AiToolRegistry toolRegistry, AiAssistantRepository assistantRepository) {
    this.aiChatClient = aiChatClient;
    this.toolRegistry = toolRegistry;
    this.assistantRepository = assistantRepository;
  }

  public AiAssistantResponse userAssistant(CurrentUser currentUser, AiAssistantMessageRequest request) {
    String content = clean(request.content());
    AiAssistantRepository.ConversationRow conversation = resolveConversation(currentUser, request.conversationId(), content);
    String memoryText = conversationMemory(conversation.id(), currentUser.userId());
    long userMessageId = assistantRepository.insertMessage(
        conversation.id(), currentUser.userId(), "user", content, List.of(), List.of(), List.of(), List.of(), false);
    assistantRepository.touchConversation(conversation.id(), userMessageId, titleFrom(content));
    ToolRun toolRun = runTools("用户端助手", currentUser, content, memoryText, conversation.id(), null, null, "user_assistant");
    List<AiSkillResult> skillResults = toolRun.results();
    List<AiAssistantStep> steps = steps(skillResults, true, toolRun.modelUsed());
    AgentReply reply = generateReply("用户端助手", content, memoryText, skillResults, fallbackReply(content, skillResults));
    boolean modelUsed = toolRun.modelUsed() || reply.modelUsed();
    List<AiAssistantCard> cards = cards(skillResults);
    List<AiAssistantAction> actions = actions(skillResults);
    List<String> usedSkills = skillResults.stream().map(AiSkillResult::name).toList();
    long assistantMessageId = assistantRepository.insertMessage(
        conversation.id(), currentUser.userId(), "assistant", reply.content(), cards, actions, steps, usedSkills, modelUsed);
    assistantRepository.touchConversation(conversation.id(), assistantMessageId, titleFrom(content));
    return new AiAssistantResponse(
        conversation.conversationNo(),
        reply.content(),
        cards,
        actions,
        steps,
        usedSkills,
        modelUsed);
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
    ToolRun toolRun = runTools("平台客服 AI", currentUser, cleaned, "", null, sessionId, relatedOrderId, "platform_support");
    List<AiSkillResult> skillResults = toolRun.results();
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

  private ToolRun runTools(
      String role,
      CurrentUser currentUser,
      String content,
      String memoryText,
      Long conversationId,
      Long supportSessionId,
      Long relatedOrderId,
      String channel) {
    List<AiChatClient.AiChatMessage> messages = List.of(
        new AiChatClient.AiChatMessage("system", """
            你是爱团应用内 AI agent 的工具规划器。你必须通过 tools 读取真实数据库信息，不要仅凭关键词或常识回答。
            根据用户问题和最近对话，选择必要且足够的工具；如果用户问店铺/团购，请优先查询店铺，再查询相关团购套餐或商品。
            如果用户问“刚才那个/第一个/继续”，根据对话记忆里的 cards 指代选择工具。
            平台客服场景下，退款、投诉、纠纷、食品安全、人工客服诉求都要查询订单/客服/治理入口等相关工具。
            不要输出普通文本，优先返回 tool_calls。
            """),
        new AiChatClient.AiChatMessage("user", """
            当前角色：%s
            当前通道：%s
            关联客服会话：%s
            关联订单：%s
            最近对话记忆：
            %s
            用户消息：%s
            """.formatted(
            role,
            channel,
            supportSessionId == null ? "无" : supportSessionId,
            relatedOrderId == null ? "无" : relatedOrderId,
            memoryText == null || memoryText.isBlank() ? "无" : memoryText,
            content)));
    Optional<List<AiToolCall>> planned = aiChatClient.toolCalls(messages, toolRegistry.definitions())
        .or(() -> aiChatClient.toolPlanJson(messages, toolRegistry.definitions()));
    boolean modelUsed = planned.isPresent();
    List<AiToolCall> calls = enrichToolCalls(
        planned.orElseGet(() -> toolRegistry.fallbackCalls(content, memoryText, channel)),
        content);
    List<AiSkillResult> results = new ArrayList<>();
    Map<String, Boolean> executed = new LinkedHashMap<>();
    for (AiToolCall call : calls) {
      if (call == null || call.name() == null || executed.containsKey(call.name())) continue;
      executed.put(call.name(), true);
      toolRegistry.execute(call, currentUser, content, memoryText, conversationId, supportSessionId, relatedOrderId, channel)
          .ifPresent(results::add);
      if (results.size() >= 14) break;
    }
    return new ToolRun(results, modelUsed);
  }

  private List<AiToolCall> enrichToolCalls(List<AiToolCall> calls, String content) {
    if (calls == null || calls.isEmpty()) return List.of();
    boolean groupBuyIntent = containsAny(content == null ? "" : content, "团购", "套餐", "到店套餐", "双人餐", "多人餐", "核销套餐");
    if (!groupBuyIntent) return calls;
    calls = calls.stream().map(call -> withGroupBuyType(call, content)).toList();
    boolean hasStore = calls.stream().anyMatch(call -> "store_lookup".equals(call.name()));
    boolean hasItem = calls.stream().anyMatch(call -> "item_lookup".equals(call.name()));
    if (hasStore == hasItem) return calls;
    List<AiToolCall> enriched = new ArrayList<>();
    if (hasItem) {
      enriched.add(new AiToolCall("enriched-store", "store_lookup", Map.of("query", content, "businessType", "group_buy")));
    }
    enriched.addAll(calls);
    if (hasStore) {
      enriched.add(new AiToolCall("enriched-item", "item_lookup", Map.of("query", content, "businessType", "group_buy")));
    }
    return enriched;
  }

  private AiToolCall withGroupBuyType(AiToolCall call, String content) {
    if (call == null || (!"store_lookup".equals(call.name()) && !"item_lookup".equals(call.name()))) return call;
    Map<String, Object> arguments = new LinkedHashMap<>();
    if (call.arguments() != null) arguments.putAll(call.arguments());
    arguments.putIfAbsent("query", content == null ? "" : content);
    arguments.put("businessType", "group_buy");
    return new AiToolCall(call.id(), call.name(), arguments);
  }

  private String fallbackReply(String content, List<AiSkillResult> skills) {
    if (!skills.isEmpty()) {
      Optional<String> groupBuyReply = groupBuyFallbackReply(skills);
      if (groupBuyReply.isPresent()) return groupBuyReply.get();
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

  private Optional<String> groupBuyFallbackReply(List<AiSkillResult> skills) {
    List<AiAssistantCard> groupBuyCards = cards(skills).stream()
        .filter(card -> ("store".equals(card.type()) || "item".equals(card.type()))
            && "group_buy".equals(payloadString(card, "businessType")))
        .toList();
    if (groupBuyCards.stream().noneMatch(card -> "store".equals(card.type()))
        || groupBuyCards.stream().noneMatch(card -> "item".equals(card.type()))) {
      return Optional.empty();
    }

    StringBuilder builder = new StringBuilder("我先按店铺把团购归好了：");
    int storeCount = 0;
    boolean appendedItem = false;
    for (AiAssistantCard card : groupBuyCards) {
      if ("store".equals(card.type())) {
        if (storeCount >= 2) break;
        storeCount++;
        appendedItem = false;
        builder.append("\n").append(storeCount).append(". ").append(card.title())
            .append("，").append(storeBrief(card));
      } else if (storeCount > 0 && storeCount <= 2) {
        builder.append(appendedItem ? "；" : "。套餐可以看：")
            .append(itemBrief(card));
        appendedItem = true;
      }
    }
    builder.append("\n我的推荐逻辑是先看距离、评分和月售，再按用餐人数和价格选套餐；下面卡片已经按店铺分组，点店铺或套餐都能直接进详情。");
    return Optional.of(limit(builder.toString(), 360));
  }

  private String storeBrief(AiAssistantCard card) {
    String content = card.content() == null ? "" : card.content();
    String[] parts = content.split(" · ");
    List<String> keep = new ArrayList<>();
    for (String part : parts) {
      if (part.contains("个套餐") || part.contains("起") || part.startsWith("评分")
          || part.startsWith("月售") || part.endsWith("m") || part.endsWith("km")) {
        keep.add(part);
      }
      if (keep.size() >= 4) break;
    }
    return keep.isEmpty() ? limit(content, 48) : String.join("、", keep);
  }

  private String itemBrief(AiAssistantCard card) {
    Object price = card.payload() == null ? null : card.payload().get("price");
    String priceText = price == null ? "" : "¥" + price;
    return priceText.isBlank() ? card.title() : card.title() + " " + priceText;
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
    return groupGroupBuyCards(cards);
  }

  private List<AiAssistantCard> groupGroupBuyCards(List<AiAssistantCard> cards) {
    Map<Long, AiAssistantCard> stores = new LinkedHashMap<>();
    Map<Long, List<AiAssistantCard>> itemsByStore = new LinkedHashMap<>();
    List<AiAssistantCard> others = new ArrayList<>();
    for (AiAssistantCard card : cards) {
      if ("store".equals(card.type()) && "group_buy".equals(payloadString(card, "businessType"))) {
        payloadLong(card, "storeId").ifPresentOrElse(
            storeId -> stores.putIfAbsent(storeId, card),
            () -> others.add(card));
      } else if ("item".equals(card.type()) && "group_buy".equals(payloadString(card, "businessType"))) {
        payloadLong(card, "storeId").ifPresentOrElse(
            storeId -> itemsByStore.computeIfAbsent(storeId, ignored -> new ArrayList<>()).add(card),
            () -> others.add(card));
      } else {
        others.add(card);
      }
    }
    if (stores.isEmpty() || itemsByStore.isEmpty()) return cards;

    List<AiAssistantCard> grouped = new ArrayList<>(others);
    for (Map.Entry<Long, AiAssistantCard> entry : stores.entrySet()) {
      grouped.add(entry.getValue());
      List<AiAssistantCard> items = new ArrayList<>(itemsByStore.getOrDefault(entry.getKey(), List.of()));
      items.sort(Comparator.comparing(card -> payloadDecimal(card, "price").orElse(BigDecimal.valueOf(Long.MAX_VALUE))));
      grouped.addAll(items);
    }
    for (Map.Entry<Long, List<AiAssistantCard>> entry : itemsByStore.entrySet()) {
      if (!stores.containsKey(entry.getKey())) grouped.addAll(entry.getValue());
    }
    return grouped;
  }

  private Optional<Long> payloadLong(AiAssistantCard card, String key) {
    if (card.payload() == null) return Optional.empty();
    Object value = card.payload().get(key);
    if (value instanceof Number number) return Optional.of(number.longValue());
    if (value instanceof String text) {
      try {
        return Optional.of(Long.parseLong(text));
      } catch (NumberFormatException ignored) {
        return Optional.empty();
      }
    }
    return Optional.empty();
  }

  private Optional<BigDecimal> payloadDecimal(AiAssistantCard card, String key) {
    if (card.payload() == null) return Optional.empty();
    Object value = card.payload().get(key);
    if (value instanceof BigDecimal number) return Optional.of(number);
    if (value instanceof Number number) return Optional.of(BigDecimal.valueOf(number.doubleValue()));
    if (value instanceof String text) {
      try {
        return Optional.of(new BigDecimal(text));
      } catch (NumberFormatException ignored) {
        return Optional.empty();
      }
    }
    return Optional.empty();
  }

  private String payloadString(AiAssistantCard card, String key) {
    if (card.payload() == null) return null;
    Object value = card.payload().get(key);
    return value == null ? null : value.toString();
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

  private List<AiAssistantStep> steps(List<AiSkillResult> skills, boolean replyStep, boolean modelPlannedTools) {
    List<AiAssistantStep> steps = new ArrayList<>();
    steps.add(new AiAssistantStep(
        modelPlannedTools ? "模型选择工具" : "本地兜底选择工具",
        modelPlannedTools ? "由 AI 根据上下文选择需要调用的业务 tool" : "模型不可用或未返回 tool call，使用保守兜底工具计划",
        "done"));
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

  record ToolRun(List<AiSkillResult> results, boolean modelUsed) {}
}
