package com.aituan.merchantcatalog.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

  @Bean
  RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .connectTimeout(java.time.Duration.ofMillis(1500))
        .readTimeout(java.time.Duration.ofMillis(1500))
        .build();
  }
}
