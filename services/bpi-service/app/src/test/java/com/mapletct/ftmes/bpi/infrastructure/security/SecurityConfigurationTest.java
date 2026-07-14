package com.mapletct.ftmes.bpi.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfiguration.class);

    @Test
    void nonWebMigrationContextDoesNotCreateServletSecurityBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SecurityConfiguration.class);
            assertThat(context).doesNotHaveBean("bpiSecurityFilterChain");
        });
    }
}
