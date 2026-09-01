package com.aituan.engagementplatform.ai;

import java.util.Optional;

interface AiSkill {
  String name();

  String description();

  Optional<AiSkillResult> evaluate(AiSkillContext context);
}

