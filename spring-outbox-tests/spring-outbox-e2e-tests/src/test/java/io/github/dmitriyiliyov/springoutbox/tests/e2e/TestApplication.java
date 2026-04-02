package io.github.dmitriyiliyov.springoutbox.tests.e2e;

import io.github.dmitriyiliyov.springoutbox.starter.EnableOutbox;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.config.BaseIntegrationTestsConfig;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.config.MySqlIntegrationTestsConfig;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.config.OracleIntegrationTestsConfig;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.config.PostgresSqlIntegrationTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableOutbox
@Import({
        BaseIntegrationTestsConfig.class,
        PostgresSqlIntegrationTestsConfig.class,
        MySqlIntegrationTestsConfig.class,
        OracleIntegrationTestsConfig.class
})
public class TestApplication { }
