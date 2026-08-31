package com.aituan.tradefulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.aituan.tradefulfillment", "com.aituan.common"})
public class TradeFulfillmentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TradeFulfillmentServiceApplication.class, args);
  }
}
