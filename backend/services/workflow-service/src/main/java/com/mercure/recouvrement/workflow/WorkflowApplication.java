package com.mercure.recouvrement.workflow;

import org.flowable.spring.boot.FlowableSecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "com.mercure.recouvrement.workflow",
                "com.deptcollector.shared"
        },

        exclude = { FlowableSecurityAutoConfiguration.class }
)
public class WorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
