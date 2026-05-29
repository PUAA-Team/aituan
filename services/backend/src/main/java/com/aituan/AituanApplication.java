package com.aituan;

import com.aituan.common.file.ImageStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(ImageStorageProperties.class)
public class AituanApplication {

  public static void main(String[] args) {
    SpringApplication.run(AituanApplication.class, args);
  }
}
