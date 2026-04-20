package io.github.dmitriyiliyov.springoutbox.web;


import io.github.dmitriyiliyov.springoutbox.web.it.config.ClockConfig;
import io.github.dmitriyiliyov.springoutbox.web.it.config.WebTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
@Import({
        ClockConfig.class,
        WebTestsConfig.class
})
public class WebTestApplication { }
