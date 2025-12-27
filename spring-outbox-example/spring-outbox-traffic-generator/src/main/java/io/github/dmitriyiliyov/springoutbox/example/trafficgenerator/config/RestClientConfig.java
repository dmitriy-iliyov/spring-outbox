package io.github.dmitriyiliyov.springoutbox.example.trafficgenerator.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class RestClientConfig {

    @Value("${traffic.generator.target.protocol}")
    public String protocol;

    @Value("${traffic.generator.target.host}")
    public String host;

    @Value("${traffic.generator.target.port}")
    public Long port;

    @Value("${traffic.generator.target.root}")
    public String root;

    public static final String BASE_URL_TEMPLATE = "%s://%s:%d%s";

    public String baseUrl;

    @PostConstruct
    public void configureBaseUrl() {
        baseUrl = BASE_URL_TEMPLATE.formatted(protocol, host, port, root);
        log.info(baseUrl);
    }

    @Bean
    public RestClient restTemplate() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
