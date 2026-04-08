package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume;

import io.github.dmitriyiliyov.springoutbox.starter.EnableOutbox;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.config.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "io.github.dmitriyiliyov.springoutbox.tests.e2e.consume",
                "io.github.dmitriyiliyov.springoutbox.tests.e2e.domain"
        }
)
@EntityScan(basePackages = "io.github.dmitriyiliyov.springoutbox.tests.e2e.domain")
@EnableJpaRepositories(basePackages = "io.github.dmitriyiliyov.springoutbox.tests.e2e.consume")
@EnableTransactionManagement
@EnableOutbox
@Import({
        PostgresSqlE2eTestsConfig.class,
        OracleE2eTestsConfig.class,
        MySqlE2eTestsConfig.class,
        KafkaE2eTestsConfig.class,
        RabbitMqE2eTestsConfig.class
})
public class ConsumeE2eTestApplication { }
