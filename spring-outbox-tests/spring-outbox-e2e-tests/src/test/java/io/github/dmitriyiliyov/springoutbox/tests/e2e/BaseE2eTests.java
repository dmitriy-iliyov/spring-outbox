package io.github.dmitriyiliyov.springoutbox.tests.e2e;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.config.DatabaseContainer;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.config.KafkaContainerSingleton;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.PublisherBusinessService;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.repository.TestOutboxRepository;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;

import static io.github.dmitriyiliyov.springoutbox.tests.e2e.config.E2eTestConfig.CONTAINER;
import static io.github.dmitriyiliyov.springoutbox.tests.e2e.config.E2eTestConfig.DATABASE_TYPE;
import static org.awaitility.Awaitility.await;

@Tag("e2e")
@SpringBootTest(classes = E2eTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseE2eTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        DatabaseContainer container = CONTAINER;
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.driver-class-name", DATABASE_TYPE::getDriverClassName);
        registry.add("spring.kafka.bootstrap-servers", KafkaContainerSingleton.INSTANCE::getBootstrapServers);
    }

    @Autowired
    protected TestOutboxRepository outboxRepository;

    @Autowired
    protected PublisherBusinessService publisherService;

    @BeforeEach
    void resetState() {
        KafkaContainerSingleton.startBroker();
        outboxRepository.truncateAll();
    }

    protected static ConditionFactory awaitAtMost(Duration timeout) {
        return await().atMost(timeout).pollInterval(Duration.ofMillis(250));
    }

    protected static ConditionFactory awaitState() {
        return awaitAtMost(Duration.ofSeconds(30));
    }
}
