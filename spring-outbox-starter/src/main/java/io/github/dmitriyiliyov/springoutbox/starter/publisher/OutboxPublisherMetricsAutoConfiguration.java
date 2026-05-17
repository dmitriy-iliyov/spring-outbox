package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass({MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnProperty(
        prefix = "outbox.publisher.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxPublisherMetricsAutoConfiguration {

    private final OutboxPublisherProperties publisherProperties;

    public OutboxPublisherMetricsAutoConfiguration(OutboxPublisherProperties publisherProperties) {
        this.publisherProperties = publisherProperties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxCache")
    public OutboxCache<EventStatus> outboxCache() {
        OutboxProperties.MetricsProperties metricsProperties = publisherProperties.getMetrics();
        if (metricsProperties != null) {
            OutboxProperties.MetricsProperties.GaugeProperties gaugeProperties = metricsProperties.getGauge();
            if (gaugeProperties != null) {
                if (gaugeProperties.isEnabled()) {
                    List<Duration> ttls = gaugeProperties.getCache().getTtls();
                    return new SimpleOutboxCache<>(
                            ttls.get(0).toSeconds(), ttls.get(1).toSeconds(), ttls.get(2).toSeconds()
                    );
                }
            }
        }
        return new NoopOutboxCache<>();
    }

    @Bean
    @Primary
    public OutboxManager outboxManagerMetricsDecorator(@Qualifier("outboxManager") OutboxManager manager,
                                                       MeterRegistry registry) {
        return new OutboxManagerMetricsDecorator(publisherProperties, registry, manager);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsRepository outboxMetricsRepository(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MultiDialectOutboxMetricsRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsService outboxMetricsService(OutboxMetricsRepository repository, OutboxCache<EventStatus> cache) {
        return new DefaultOutboxMetricsService(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxMetrics")
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxMetrics(MeterRegistry registry, OutboxMetricsService metricsService) {
        return new DefaultOutboxMetrics(publisherProperties, registry, metricsService);
    }
}
