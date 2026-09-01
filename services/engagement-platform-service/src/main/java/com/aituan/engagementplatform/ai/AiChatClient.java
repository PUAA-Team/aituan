package com.aituan.engagementplatform.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class AiChatClient {
  private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
  private static final TypeReference<List<JsonToolCall>> JSON_TOOL_CALLS = new TypeReference<>() {};
  private final AiProperties properties;
  private final ObjectMapper objectMapper;

  AiChatClient(AiProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  Optional<String> chat(List<AiChatMessage> messages) {
    if (!properties.usable()) return Optional.empty();
    try {
      ChatCompletionResponse response = restClient(properties.timeout())
          .post()
          .uri(chatUri())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + properties.getApiKey())
          .body(Map.of(
              "model", properties.getModel(),
              "messages", messages,
              "temperature", properties.getTemperature(),
              "max_tokens", properties.getMaxTokens()))
          .retrieve()
          .body(ChatCompletionResponse.class);
      if (response == null || response.choices() == null || response.choices().isEmpty()) {
        return Optional.empty();
      }
      ChatMessage message = response.choices().get(0).message();
      if (message == null || message.content() == null || message.content().isBlank()) {
        return Optional.empty();
      }
      return Optional.of(message.content().trim());
    } catch (IllegalArgumentException | RestClientException ex) {
      return Optional.empty();
    }
  }

  Optional<List<AiToolCall>> toolCalls(List<AiChatMessage> messages, List<AiToolDefinition> tools) {
    if (!properties.usable() || tools.isEmpty()) return Optional.empty();
    try {
      ChatCompletionResponse response = restClient(properties.timeout())
          .post()
          .uri(chatUri())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + properties.getApiKey())
          .body(Map.of(
              "model", properties.getModel(),
              "messages", messages,
              "tools", openAiTools(tools),
              "tool_choice", "auto",
              "temperature", 0,
              "max_tokens", Math.min(properties.getMaxTokens(), 600)))
          .retrieve()
          .body(ChatCompletionResponse.class);
      if (response == null || response.choices() == null || response.choices().isEmpty()) {
        return Optional.empty();
      }
      ChatMessage message = response.choices().get(0).message();
      if (message == null || message.toolCalls() == null || message.toolCalls().isEmpty()) {
        return Optional.empty();
      }
      List<AiToolCall> calls = new ArrayList<>();
      for (ChatToolCall toolCall : message.toolCalls()) {
        if (toolCall == null || toolCall.function() == null || toolCall.function().name() == null) continue;
        calls.add(new AiToolCall(
            toolCall.id() == null || toolCall.id().isBlank() ? "tool-" + calls.size() : toolCall.id(),
            toolCall.function().name(),
            readArguments(toolCall.function().arguments())));
      }
      return calls.isEmpty() ? Optional.empty() : Optional.of(calls);
    } catch (IllegalArgumentException | RestClientException ex) {
      return Optional.empty();
    }
  }

  Optional<List<AiToolCall>> toolPlanJson(List<AiChatMessage> messages, List<AiToolDefinition> tools) {
    if (!properties.usable() || tools.isEmpty()) return Optional.empty();
    String toolText = tools.stream()
        .map(tool -> "- " + tool.name() + ": " + tool.description())
        .reduce((left, right) -> left + "\n" + right)
        .orElse("");
    List<AiChatMessage> jsonMessages = List.of(
        new AiChatMessage("system", """
            你是爱团 AI agent 的工具规划器。你只能输出 JSON 数组，不要输出解释。
            数组元素格式：{"name":"tool_name","arguments":{"query":"用户问题","businessType":"可选业务类型","limit":6}}
            必须从可用工具中选择必要且足够的工具；用户问店铺/团购时优先 search 店铺类工具再查商品/套餐类工具。
            可用工具：
            %s
            """.formatted(toolText)),
        new AiChatMessage("user", messages.stream()
            .map(message -> message.role() + ": " + message.content())
            .reduce((left, right) -> left + "\n" + right)
            .orElse("")));
    return chat(jsonMessages).flatMap(this::parseJsonToolPlan);
  }

  private RestClient restClient(Duration timeout) {
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(timeout);
    return RestClient.builder().requestFactory(factory).build();
  }

  private URI chatUri() {
    String base = properties.getApiUrl().trim();
    String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    if (normalized.endsWith("/v1/chat/completions")) {
      return URI.create(normalized);
    }
    return URI.create(normalized + "/v1/chat/completions");
  }

  private List<Map<String, Object>> openAiTools(List<AiToolDefinition> tools) {
    return tools.stream()
        .map(tool -> {
          Map<String, Object> function = new LinkedHashMap<>();
          function.put("name", tool.name());
          function.put("description", tool.description());
          function.put("parameters", tool.parameters());
          Map<String, Object> wrapper = new LinkedHashMap<>();
          wrapper.put("type", "function");
          wrapper.put("function", function);
          return wrapper;
        })
        .toList();
  }

  private Map<String, Object> readArguments(String json) {
    if (json == null || json.isBlank()) return Map.of();
    try {
      return objectMapper.readValue(json, MAP);
    } catch (JsonProcessingException ex) {
      return Map.of("query", json);
    }
  }

  private Optional<List<AiToolCall>> parseJsonToolPlan(String content) {
    String json = jsonArray(content);
    if (json.isBlank()) return Optional.empty();
    try {
      List<JsonToolCall> rows = objectMapper.readValue(json, JSON_TOOL_CALLS);
      List<AiToolCall> calls = new ArrayList<>();
      for (JsonToolCall row : rows) {
        if (row == null || row.name() == null || row.name().isBlank()) continue;
        calls.add(new AiToolCall("json-" + calls.size(), row.name(), row.arguments() == null ? Map.of() : row.arguments()));
      }
      return calls.isEmpty() ? Optional.empty() : Optional.of(calls);
    } catch (JsonProcessingException ex) {
      return Optional.empty();
    }
  }

  private String jsonArray(String content) {
    if (content == null) return "";
    int start = content.indexOf('[');
    int end = content.lastIndexOf(']');
    if (start < 0 || end < start) return "";
    return content.substring(start, end + 1);
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record AiChatMessage(String role, String content) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatCompletionResponse(List<Choice> choices) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Choice(ChatMessage message) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatMessage(
      String content,
      @JsonProperty("tool_calls") List<ChatToolCall> toolCalls) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatToolCall(
      String id,
      String type,
      ChatToolFunction function) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatToolFunction(
      String name,
      String arguments) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record JsonToolCall(
      String name,
      Map<String, Object> arguments) {}
}
