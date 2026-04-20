package io.github.dmitriyiliyov.springoutbox.web.it.config;

import io.github.dmitriyiliyov.springoutbox.web.DlqStatusQueryConverter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class WebTestsConfig {

    @Bean
    public DlqStatusQueryConverter dlqStatusQueryConverter() {
        return new DlqStatusQueryConverter();
    }
}
