package com.deptcollector.legal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class LegalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegalServiceApplication.class, args);
    }
}
