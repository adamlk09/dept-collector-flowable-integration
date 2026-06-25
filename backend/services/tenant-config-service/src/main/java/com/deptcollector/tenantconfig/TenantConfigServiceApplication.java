package com.deptcollector.tenantconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class TenantConfigServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantConfigServiceApplication.class, args);
    }
}
