package com.deptcollector.kpi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class KpiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KpiServiceApplication.class, args);
    }
}
