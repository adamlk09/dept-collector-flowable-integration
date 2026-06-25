package com.deptcollector.audit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AuditServiceApplicationTest {
    @Test
    void applicationClassExists() {
        assertThat(AuditServiceApplication.class).isNotNull();
    }
}
