package com.aituan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AituanApplication {

  public static void main(String[] args) {
    SpringApplication.run(AituanApplication.class, args);
  }
}
