package io.github.dmitriyiliyov.oncebox.dlq.api.it.config;

import io.github.dmitriyiliyov.oncebox.dlq.api.DlqStatusQueryConverter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class WebTestsConfig {

    @Bean
    public DlqStatusQueryConverter dlqStatusQueryConverter() {
        return new DlqStatusQueryConverter();
    }
}
