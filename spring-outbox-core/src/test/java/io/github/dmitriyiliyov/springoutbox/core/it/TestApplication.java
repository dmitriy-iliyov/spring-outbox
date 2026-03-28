package io.github.dmitriyiliyov.springoutbox.core.it;

import io.github.dmitriyiliyov.springoutbox.core.it.config.BaseIntegrationTestsConfig;
import io.github.dmitriyiliyov.springoutbox.core.it.config.MySqlIntegrationTestsConfig;
import io.github.dmitriyiliyov.springoutbox.core.it.config.OracleIntegrationTestsConfig;
import io.github.dmitriyiliyov.springoutbox.core.it.config.PostgresSqlIntegrationTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@Import({
        BaseIntegrationTestsConfig.class,
        PostgresSqlIntegrationTestsConfig.class,
        MySqlIntegrationTestsConfig.class,
        OracleIntegrationTestsConfig.class
})
public class TestApplication { }
