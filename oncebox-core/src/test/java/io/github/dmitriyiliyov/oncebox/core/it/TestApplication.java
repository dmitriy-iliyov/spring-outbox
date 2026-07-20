package io.github.dmitriyiliyov.oncebox.core.it;

import io.github.dmitriyiliyov.oncebox.core.it.config.BaseIntegrationTestsConfig;
import io.github.dmitriyiliyov.oncebox.core.it.config.MySqlIntegrationTestsConfig;
import io.github.dmitriyiliyov.oncebox.core.it.config.OracleIntegrationTestsConfig;
import io.github.dmitriyiliyov.oncebox.core.it.config.PostgresSqlIntegrationTestsConfig;
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
