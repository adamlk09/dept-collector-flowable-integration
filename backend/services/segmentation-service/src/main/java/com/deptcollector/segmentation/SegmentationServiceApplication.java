package com.deptcollector.segmentation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class SegmentationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SegmentationServiceApplication.class, args);
    }
}
