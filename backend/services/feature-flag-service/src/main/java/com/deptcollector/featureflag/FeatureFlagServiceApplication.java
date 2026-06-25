package com.deptcollector.featureflag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class FeatureFlagServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeatureFlagServiceApplication.class, args);
    }
}
