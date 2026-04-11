package io.github.dmitriyiliyov.springoutbox.tests.integration.publish;

import io.github.dmitriyiliyov.springoutbox.starter.EnableOutbox;
import io.github.dmitriyiliyov.springoutbox.tests.integration.publish.config.OutboxIntegrationTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "io.github.dmitriyiliyov.springoutbox.tests.integration.publish",
                "io.github.dmitriyiliyov.springoutbox.tests.integration.domain"
        }
)
@EntityScan(basePackages = "io.github.dmitriyiliyov.springoutbox.tests.integration.domain")
@EnableJpaRepositories(basePackages = "io.github.dmitriyiliyov.springoutbox.tests.integration.publish")
@EnableTransactionManagement
@EnableOutbox
@Import({
        OutboxIntegrationTestsConfig.class
})
public class OutboxIntegrationTestApplication { }
