package com.aituan.ai;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
class AiSkillRegistry {
  private final List<AiSkill> skills;

  AiSkillRegistry(List<AiSkill> skills) {
    this.skills = skills;
  }

  List<AiSkillResult> evaluate(AiSkillContext context) {
    return skills.stream()
        .map(skill -> skill.evaluate(context))
        .flatMap(java.util.Optional::stream)
        .toList();
  }
}
