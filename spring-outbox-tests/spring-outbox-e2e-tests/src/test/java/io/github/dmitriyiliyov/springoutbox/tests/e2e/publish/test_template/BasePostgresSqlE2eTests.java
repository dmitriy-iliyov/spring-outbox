package io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.test_template;

import io.github.dmitriyiliyov.springoutbox.tests.utils.PostgresTestContainerSingleton;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


@ActiveProfiles({"postgres-e2e", "publish-e2e"})
public abstract class BasePostgresSqlE2eTests extends BaseE2eTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainerSingleton.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainerSingleton.INSTANCE::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainerSingleton.INSTANCE::getPassword);
    }
}
