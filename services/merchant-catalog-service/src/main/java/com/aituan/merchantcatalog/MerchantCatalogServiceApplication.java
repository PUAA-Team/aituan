package com.aituan.merchantcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.aituan.merchantcatalog", "com.aituan.common"})
public class MerchantCatalogServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(MerchantCatalogServiceApplication.class, args);
  }
}
