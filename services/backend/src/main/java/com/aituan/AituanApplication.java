package com.aituan;

import com.aituan.common.file.ImageStorageProperties;
import com.aituan.ai.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({ImageStorageProperties.class, AiProperties.class})
public class AituanApplication {

  public static void main(String[] args) {
    SpringApplication.run(AituanApplication.class, args);
  }
}
