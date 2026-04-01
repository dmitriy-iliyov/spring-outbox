package io.github.dmitriyiliyov.springoutbox.metrics.it_config;

import io.github.dmitriyiliyov.springoutbox.tests.utils.MySqlTestContainerSingleton;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("mysql-it")
public abstract class BaseMySqlIntegrationTests extends BaseIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySqlTestContainerSingleton.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", MySqlTestContainerSingleton.INSTANCE::getUsername);
        registry.add("spring.datasource.password", MySqlTestContainerSingleton.INSTANCE::getPassword);
    }
}
