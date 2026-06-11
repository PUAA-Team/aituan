package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.params;

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
    if (AiSkillSupport.containsAny(text, "投诉", "举报", "纠纷", "食品安全", "服务态度", "差评")) {
      return Optional.of(new AiSkillResult(
          name(),
          "投诉举报入口",
          "建议准备订单号、问题发生时间、文字说明和图片证据；食品安全、退款纠纷、商家服务争议可提交投诉工单，紧急问题直接转人工。",
          List.of(new AiAssistantCard("complaint", "投诉与建议", "提交工单后平台可跟踪处理进度并保留证据。", "提交投诉", "/complaint/submit", params())),
          List.of(
              new AiAssistantAction("提交投诉", null, "/complaint/submit", params()),
              new AiAssistantAction("转平台人工", "我要转人工", "/support/sessions", params()))));
    }
    if (AiSkillSupport.containsAny(text, "人工", "客服", "平台介入", "没人回", "转人工")) {
      return Optional.of(new AiSkillResult(
          name(),
          "平台客服入口",
          "可进入平台客服会话并点击转人工，或直接发送“转人工”。AI 会先整理订单、投诉和客服信息给人工查看。",
          List.of(new AiAssistantCard("support", "平台客服", "AI 会先整理问题，用户可随时转人工。", "打开客服", "/support/sessions", params())),
          List.of(new AiAssistantAction("打开客服", null, "/support/sessions", params()))));
    }
    return Optional.empty();
  }
}
