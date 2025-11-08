package io.github.dmitriyiliyov.springoutbox.integration;

import io.github.dmitriyiliyov.springoutbox.config.OutboxAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.consumer.config.OutboxConsumerAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.integration.config.TestConfiguration;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxDlqAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {
        OutboxAutoConfiguration.class,
        OutboxPublisherAutoConfiguration.class,
        OutboxDlqAutoConfiguration.class,
        OutboxConsumerAutoConfiguration.class,
        TestConfiguration.class
})
public class SpringBootContextIntegrationTests {

    @Test
    @DisplayName("IT load context")
    public void loadContext() {}
}
