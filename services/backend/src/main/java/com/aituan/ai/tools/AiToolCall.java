package com.aituan.ai;

import java.util.Map;

record AiToolCall(
    String id,
    String name,
    Map<String, Object> arguments) {}
