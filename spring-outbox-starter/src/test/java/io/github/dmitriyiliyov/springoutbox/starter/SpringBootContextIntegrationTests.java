package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxDlqAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherAutoConfiguration;
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
