package io.github.dmitriyiliyov.springoutbox.starter.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqApiService;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqController;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.OutboxDlqApiServiceMetricsDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.publisher.dlq",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({OutboxDlqController.class, MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnProperty(
        prefix = "outbox.publisher.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxDlqApiMetricsAutoConfiguration {

    @Bean
    @Primary
    public OutboxDlqApiService outboxDlqApiServiceMetricsDecorator(@Qualifier("outboxDlqApiService") OutboxDlqApiService service,
                                                                   MeterRegistry registry) {
        return new OutboxDlqApiServiceMetricsDecorator(registry, service);
    }
}
