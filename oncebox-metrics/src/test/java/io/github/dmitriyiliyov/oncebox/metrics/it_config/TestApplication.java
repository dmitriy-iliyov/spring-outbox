package io.github.dmitriyiliyov.oncebox.metrics.it_config;

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
