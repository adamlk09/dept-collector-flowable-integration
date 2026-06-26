package com.mercure.recouvrement.workflow.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Override
    public void configure(SpringProcessEngineConfiguration config) {
        config.setAsyncExecutorActivate(true);
        config.setEnableSafeBpmnXml(false);
    }
}
