package com.deptcollector.datacontract;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.deptcollector")
public class DataContractServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataContractServiceApplication.class, args);
    }
}
