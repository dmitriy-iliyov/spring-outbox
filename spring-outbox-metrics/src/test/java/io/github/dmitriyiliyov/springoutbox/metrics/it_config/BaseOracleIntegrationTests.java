package io.github.dmitriyiliyov.springoutbox.metrics.it_config;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("oracle-it")
public abstract class BaseOracleIntegrationTests extends BaseIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (!BaseIntegrationTests.isCi()) {
            registry.add("spring.datasource.url", OracleTestContainerSingleton.INSTANCE::getJdbcUrl);
            registry.add("spring.datasource.username", OracleTestContainerSingleton.INSTANCE::getUsername);
            registry.add("spring.datasource.password", OracleTestContainerSingleton.INSTANCE::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
        }
    }
}