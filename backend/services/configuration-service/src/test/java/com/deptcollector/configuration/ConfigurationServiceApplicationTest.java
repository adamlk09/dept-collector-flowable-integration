package com.deptcollector.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ConfigurationServiceApplicationTest {
    @Test
    void applicationClassExists() {
        assertThat(ConfigurationServiceApplication.class).isNotNull();
    }
}
