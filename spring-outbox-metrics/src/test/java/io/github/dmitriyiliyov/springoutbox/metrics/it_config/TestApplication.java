package io.github.dmitriyiliyov.springoutbox.metrics.it_config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        BaseIntegrationTestsConfig.class,
        MySqlIntegrationTestsConfig.class,
        OracleIntegrationTestsConfig.class,
        PostgresSqlIntegrationTestsConfig.class,
})
public class TestApplication { }
