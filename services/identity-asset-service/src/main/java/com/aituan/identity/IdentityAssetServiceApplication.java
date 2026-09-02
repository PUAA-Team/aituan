package com.aituan.identity;

import com.aituan.identity.file.IdentityUploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.aituan.identity", "com.aituan.common"})
@EnableConfigurationProperties(IdentityUploadProperties.class)
public class IdentityAssetServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(IdentityAssetServiceApplication.class, args);
  }
}
