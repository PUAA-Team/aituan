package com.aituan.engagementplatform;

import com.aituan.engagementplatform.ai.AiProperties;
import com.aituan.engagementplatform.file.ImageStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = "com.aituan",
    exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@EnableConfigurationProperties({AiProperties.class, ImageStorageProperties.class})
public class EngagementPlatformServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EngagementPlatformServiceApplication.class, args);
  }
}
