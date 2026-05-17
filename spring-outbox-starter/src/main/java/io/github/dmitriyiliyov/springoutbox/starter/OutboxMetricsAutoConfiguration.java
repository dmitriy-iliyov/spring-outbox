package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@Configuration
@ConditionalOnClass({MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnAnyMetricsEnabled
public class OutboxMetricsAutoConfiguration {

    @Bean
    @Primary
    public OutboxScheduleStrategyListenerSupplier metricsOutboxScheduleStrategyListenerSupplier(MeterRegistry registry) {
        return new MetricsOutboxScheduleStrategyListenerSupplier(registry);
    }

    @Bean
    @Primary
    public ContinuableTaskDecoratorSupplier metricsContinuableTaskDecoratorSupplier(MeterRegistry registry) {
        return new ContinuableTaskTimeMeasureDecoratorSupplier(registry);
    }

    @Bean
    public PostApplicationReadyOutboxInitializer metricsOutboxInitializer(Map<String, OutboxMetrics> metrics) {
        return new MetricsPostApplicationReadyOutboxInitializer(metrics);
    }
}
