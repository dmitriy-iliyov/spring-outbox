package io.github.dmitriyiliyov.springoutbox.integration;

import io.github.dmitriyiliyov.springoutbox.config.OutboxAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.config.OutboxDlqAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.integration.config.TestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {
        OutboxAutoConfiguration.class,
        OutboxDlqAutoConfiguration.class,
        TestConfiguration.class
})
public class SpringBootContextIntegrationTests {

    @Test
    @DisplayName("IT load context")
    public void loadContext() {}
}
