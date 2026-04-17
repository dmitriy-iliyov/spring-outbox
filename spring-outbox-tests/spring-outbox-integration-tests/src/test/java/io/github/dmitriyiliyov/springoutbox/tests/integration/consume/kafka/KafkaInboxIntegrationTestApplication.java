package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.springoutbox.starter.EnableOutbox;
import io.github.dmitriyiliyov.springoutbox.tests.integration.ClockConfig;
import io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared.InboxIntegrationTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared",
                "io.github.dmitriyiliyov.springoutbox.tests.integration.consume.kafka",
                "io.github.dmitriyiliyov.springoutbox.tests.integration.domain"
        }
)
@EntityScan(basePackages = "io.github.dmitriyiliyov.springoutbox.tests.integration.domain")
@EnableJpaRepositories(basePackages = "io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared")
@EnableTransactionManagement
@EnableOutbox
@Import({
        InboxIntegrationTestsConfig.class,
        KafkaIntegrationTestsConfig.class,
        ClockConfig.class
})
public class KafkaInboxIntegrationTestApplication { }
