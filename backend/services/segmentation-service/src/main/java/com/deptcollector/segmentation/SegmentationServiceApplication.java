package com.deptcollector.segmentation;

import org.flowable.spring.boot.FlowableSecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// FlowableSecurityAutoConfiguration excluded the same way workflow-service excludes it for its
// own -rest starter: /dmn-api stays under the shared-kernel security chain (TenantContextFilter
// exemption), not Flowable's own basic-auth/IDM.
@SpringBootApplication(scanBasePackages = "com.deptcollector", exclude = { FlowableSecurityAutoConfiguration.class })
public class SegmentationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SegmentationServiceApplication.class, args);
    }
}
