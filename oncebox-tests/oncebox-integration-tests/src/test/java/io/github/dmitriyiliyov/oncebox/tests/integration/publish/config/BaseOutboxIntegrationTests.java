package io.github.dmitriyiliyov.oncebox.tests.integration.publish.config;

import io.github.dmitriyiliyov.oncebox.tests.integration.publish.OutboxIntegrationTestApplication;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;

import static io.github.dmitriyiliyov.oncebox.tests.integration.publish.config.OutboxIntegrationTestsConfig.CONTAINER;
import static io.github.dmitriyiliyov.oncebox.tests.integration.publish.config.OutboxIntegrationTestsConfig.DATABASE_TYPE;

@Tag("outbox")
@SpringBootTest(classes = OutboxIntegrationTestApplication.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public abstract class BaseOutboxIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER::getUsername);
        registry.add("spring.datasource.password", CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", DATABASE_TYPE::getDriverClassName);
    }
}
