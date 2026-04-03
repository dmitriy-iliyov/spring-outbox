package io.github.dmitriyiliyov.springoutbox.tests.e2e.test_template;

import io.github.dmitriyiliyov.springoutbox.tests.utils.PostgresTestContainerSingleton;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


@ActiveProfiles("postgres-it")
public abstract class BasePostgresSqlIntegrationTests extends BaseIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainerSingleton.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainerSingleton.INSTANCE::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainerSingleton.INSTANCE::getPassword);
    }
}
