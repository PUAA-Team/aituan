package com.aituan.support;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
class AiSupportService {
  private final RestClient restClient;
  private final String apiKey;
  private final String model;

  AiSupportService(
      RestClient.Builder builder,
      @Value("${aituan.openai.api-key:${OPENAI_API_KEY:}}") String apiKey,
      @Value("${aituan.openai.model:${OPENAI_MODEL:gpt-4o-mini}}") String model) {
    this.restClient = builder.baseUrl("https://api.openai.com").build();
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model == null || model.isBlank() ? "gpt-4o-mini" : model.trim();
  }

  String reply(String content) {
    if (apiKey.isBlank()) {
      return fallback(content);
    }
    try {
      Map<String, Object> response = restClient.post()
          .uri("/v1/responses")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of(
              "model", model,
              "input", List.of(
                  Map.of("role", "system", "content", "你是爱团平台客服助手。请用简短中文回复，先确认问题并提示可转人工。"),
                  Map.of("role", "user", "content", content))))
          .retrieve()
          .body(Map.class);
      String text = extractText(response);
      return text == null || text.isBlank() ? fallback(content) : text.trim();
    } catch (RestClientException | IllegalArgumentException ex) {
      return fallback(content);
    }
  }

  private String fallback(String content) {
    String text = content == null ? "" : content;
    if (containsAny(text, "投诉", "差评", "纠纷")) {
      return "我已记录您的投诉诉求。您可以点右上角投诉入口提交工单，也可以发送“转人工”让平台客服接入。";
    }
    if (containsAny(text, "退款", "退单", "取消")) {
      return "我已记录退款/取消问题。请补充订单信息，必要时可发送“转人工”让平台客服继续处理。";
    }
    return "平台客服助手已收到。请继续补充订单或问题细节；如需人工处理，可点击“转人工”或直接发送“转人工”。";
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private String extractText(Map<String, Object> response) {
    if (response == null) return null;
    Object outputText = response.get("output_text");
    if (outputText instanceof String text) return text;
    Object output = response.get("output");
    if (!(output instanceof List<?> outputList)) return null;
    for (Object item : outputList) {
      if (!(item instanceof Map<?, ?> itemMap)) continue;
      Object content = itemMap.get("content");
      if (!(content instanceof List<?> contentList)) continue;
      for (Object part : contentList) {
        if (!(part instanceof Map<?, ?> partMap)) continue;
        Object text = partMap.get("text");
        if (text instanceof String value && !value.isBlank()) return value;
      }
    }
    return null;
  }
}
