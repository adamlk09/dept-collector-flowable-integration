package com.deptcollector.cheque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class ChequeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChequeServiceApplication.class, args);
    }
}
