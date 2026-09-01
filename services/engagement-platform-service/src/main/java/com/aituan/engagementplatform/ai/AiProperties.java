package com.aituan.engagementplatform.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aituan.ai")
public class AiProperties {
  private boolean enabled;
  private String apiUrl = "";
  private String apiKey = "";
  private String model = "";
  private int timeoutSeconds = 20;
  private int maxTokens = 800;
  private double temperature = 0.25;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public int getMaxTokens() {
    return maxTokens;
  }

  public void setMaxTokens(int maxTokens) {
    this.maxTokens = maxTokens;
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }

  Duration timeout() {
    return Duration.ofSeconds(Math.max(3, timeoutSeconds));
  }

  boolean usable() {
    return enabled
        && apiUrl != null
        && !apiUrl.isBlank()
        && apiKey != null
        && !apiKey.isBlank()
        && model != null
        && !model.isBlank();
  }
}
