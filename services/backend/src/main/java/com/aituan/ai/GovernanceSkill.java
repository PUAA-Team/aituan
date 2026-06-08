package com.aituan.ai;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class GovernanceSkill implements AiSkill {
  @Override
  public String name() {
    return "governance_entry";
  }

  @Override
  public String description() {
    return "识别投诉、举报、评价争议和转人工诉求，提供正确入口与证据清单";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    String text = context.normalizedMessage();
    if (containsAny(text, "投诉", "举报", "纠纷", "食品安全", "服务态度", "差评")) {
      return Optional.of(new AiSkillResult(
          name(),
          "投诉举报入口",
          "用户有投诉/举报诉求。建议先收集订单号、问题时间、文字说明和图片证据，再引导提交投诉工单；紧急或高风险问题建议转人工。",
          List.of(new AiAssistantCard("complaint", "投诉与建议", "提交工单后平台可跟踪处理进度并保留证据。", "提交投诉", "/complaint/submit", java.util.Map.of())),
          List.of(
              new AiAssistantAction("提交投诉", null, "/complaint/submit", java.util.Map.of()),
              new AiAssistantAction("转平台人工", "我要转人工", "/support/sessions", java.util.Map.of()))));
    }
    if (containsAny(text, "评价", "评分", "点赞", "图片无法显示")) {
      return Optional.of(new AiSkillResult(
          name(),
          "评价助手",
          "用户在评价相关流程遇到问题。可提示进入“我的评价”查看详情、点赞或举报；若涉及商家纠纷，引导从评价详情或投诉入口提交证据。",
          List.of(new AiAssistantCard("review", "我的评价", "查看已发布评价、评价详情、点赞和举报入口。", "查看评价", "/review/my", java.util.Map.of())),
          List.of(new AiAssistantAction("查看我的评价", null, "/review/my", java.util.Map.of()))));
    }
    if (containsAny(text, "人工", "客服", "平台介入", "没人回")) {
      return Optional.of(new AiSkillResult(
          name(),
          "平台客服入口",
          "用户有人工客服诉求。建议进入平台客服会话并点击转人工，或直接发送“转人工”。",
          List.of(new AiAssistantCard("support", "平台客服", "AI 会先整理问题，用户可随时转人工。", "打开客服", "/support/sessions", java.util.Map.of())),
          List.of(new AiAssistantAction("打开客服", null, "/support/sessions", java.util.Map.of()))));
    }
    return Optional.empty();
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }
}
