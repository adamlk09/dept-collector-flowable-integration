package com.deptcollector.contract;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ContractServiceApplicationTest {
    @Test
    void applicationClassExists() {
        assertThat(ContractServiceApplication.class).isNotNull();
    }
}
