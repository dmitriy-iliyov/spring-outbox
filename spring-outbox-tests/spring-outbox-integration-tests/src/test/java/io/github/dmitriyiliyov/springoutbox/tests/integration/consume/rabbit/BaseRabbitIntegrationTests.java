package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.rabbit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;

import static io.github.dmitriyiliyov.springoutbox.tests.integration.publish.config.OutboxIntegrationTestsConfig.CONTAINER;
import static io.github.dmitriyiliyov.springoutbox.tests.integration.publish.config.OutboxIntegrationTestsConfig.DATABASE_TYPE;

@Tag("rabbit-inbox")
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(classes = RabbitInboxIntegrationTestApplication.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseRabbitIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER::getUsername);
        registry.add("spring.datasource.password", CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", DATABASE_TYPE::getDriverClassName);
    }
}
