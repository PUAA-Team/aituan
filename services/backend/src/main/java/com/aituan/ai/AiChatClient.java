package com.aituan.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.time.Duration;
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
  private final AiProperties properties;

  AiChatClient(AiProperties properties) {
    this.properties = properties;
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

  record AiChatMessage(String role, String content) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatCompletionResponse(List<Choice> choices) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Choice(ChatMessage message) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatMessage(String content) {}
}
