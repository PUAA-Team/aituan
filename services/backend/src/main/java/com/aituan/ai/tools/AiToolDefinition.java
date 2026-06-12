package com.aituan.ai;

import java.util.Map;

record AiToolDefinition(
    String name,
    String description,
    Map<String, Object> parameters) {}
