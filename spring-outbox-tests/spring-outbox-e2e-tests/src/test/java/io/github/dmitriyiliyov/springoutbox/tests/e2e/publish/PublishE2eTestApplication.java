package io.github.dmitriyiliyov.springoutbox.tests.e2e.publish;

import io.github.dmitriyiliyov.springoutbox.starter.EnableOutbox;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.config.MySqlE2eTestsConfig;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.config.OracleE2eTestsConfig;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.config.PostgresSqlE2eTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Profile("publish-e2e")
@SpringBootApplication(
        scanBasePackages = {
                "io.github.dmitriyiliyov.springoutbox.tests.e2e.publish",
                "io.github.dmitriyiliyov.springoutbox.tests.e2e.domain"
        }
)
@EntityScan(basePackages = "io.github.dmitriyiliyov.springoutbox.tests.e2e.domain")
@EnableJpaRepositories(basePackages = "io.github.dmitriyiliyov.springoutbox.tests.e2e.publish")
@EnableTransactionManagement
@EnableOutbox
@Import({
        PostgresSqlE2eTestsConfig.class,
        MySqlE2eTestsConfig.class,
        OracleE2eTestsConfig.class
})
public class PublishE2eTestApplication { }
