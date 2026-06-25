package com.deptcollector.agency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class AgencyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgencyServiceApplication.class, args);
    }
}
