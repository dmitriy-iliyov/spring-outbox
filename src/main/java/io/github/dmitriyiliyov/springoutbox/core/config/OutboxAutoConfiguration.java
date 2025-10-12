package io.github.dmitriyiliyov.springoutbox.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.DefaultOutboxProcessor;
import io.github.dmitriyiliyov.springoutbox.DefaultOutboxPublisher;
import io.github.dmitriyiliyov.springoutbox.JacksonOutboxSerializer;
import io.github.dmitriyiliyov.springoutbox.PostgreSqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisher;
import io.github.dmitriyiliyov.springoutbox.core.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.OutboxSerializer;
import io.github.dmitriyiliyov.springoutbox.utils.UuidV7Generator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxAutoConfiguration {

    private final OutboxProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper;

    public OutboxAutoConfiguration(OutboxProperties properties, JdbcTemplate jdbcTemplate, ObjectMapper mapper) {
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Bean
    public OutboxRepository outboxRepository() {
        return new PostgreSqlOutboxRepository(jdbcTemplate);
    }

    @Bean
    public OutboxScheduler outboxScheduler(OutboxRepository repository) {
        DefaultOutboxProcessor processor = new DefaultOutboxProcessor(repository);
        return new OutboxScheduler();
    }

    @Bean
    public OutboxSerializer outboxSerializer() {
        return new JacksonOutboxSerializer(mapper, new UuidV7Generator());
    }

    @Bean
    public OutboxPublisher outboxPublisher(OutboxSerializer serializer, OutboxRepository repository) {
        return new DefaultOutboxPublisher(properties, serializer, repository);
    }
}
