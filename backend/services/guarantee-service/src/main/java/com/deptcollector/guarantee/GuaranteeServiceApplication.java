package com.deptcollector.guarantee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class GuaranteeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuaranteeServiceApplication.class, args);
    }
}
