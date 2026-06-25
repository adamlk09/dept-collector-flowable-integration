package com.deptcollector.debt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class DebtServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DebtServiceApplication.class, args);
    }
}
