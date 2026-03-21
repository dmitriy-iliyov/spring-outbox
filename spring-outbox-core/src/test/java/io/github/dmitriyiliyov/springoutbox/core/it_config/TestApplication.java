package io.github.dmitriyiliyov.springoutbox.core.it_config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        BaseIntegrationTestsConfig.class,
        PostgresSqlIntegrationTestsConfig.class,
        MySqlIntegrationTestsConfig.class,
        OracleIntegrationTestsConfig.class
})
public class TestApplication { }
