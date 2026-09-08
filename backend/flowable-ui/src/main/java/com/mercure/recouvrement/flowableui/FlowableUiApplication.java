package com.mercure.recouvrement.flowableui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Flowable UI (Modeler / Task / Admin / IDM).
 *
 * <p>This application is a <b>workflow designer only</b> — it contains no business code, no custom
 * {@code RuntimeService}/{@code TaskService} APIs and no workflow engine of its own beyond the one
 * the Flowable UI starters embed. The actual collection engine lives in {@code workflow-service}.
 *
 * <p>All four UI apps are activated purely through the {@code flowable-spring-boot-starter-ui-*}
 * auto-configurations on the classpath, so a plain {@code @SpringBootApplication} is enough — there
 * is nothing to component-scan in this package.
 *
 * <p>The apps are served on one port under separate context paths:
 * {@code /flowable-modeler}, {@code /flowable-task}, {@code /flowable-admin}, {@code /flowable-idm}.
 */
@SpringBootApplication
public class FlowableUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowableUiApplication.class, args);
    }
}
