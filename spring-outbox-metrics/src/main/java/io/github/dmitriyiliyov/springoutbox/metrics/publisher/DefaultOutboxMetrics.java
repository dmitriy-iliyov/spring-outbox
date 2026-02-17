package io.github.dmitriyiliyov.springoutbox.metrics.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;

public final class DefaultOutboxMetrics implements OutboxMetrics {

    private final OutboxPublisherPropertiesHolder properties;
    private final MeterRegistry registry;
    private final OutboxMetricsService metricsService;
    private final EventStatus [] statuses = new EventStatus[] {EventStatus.PENDING, EventStatus.IN_PROCESS};

    public DefaultOutboxMetrics(OutboxPublisherPropertiesHolder properties,
                                MeterRegistry registry,
                                OutboxMetricsService metricsService) {
        this.properties = properties;
        this.registry = registry;
        this.metricsService = metricsService;
    }

    @Override
    public void register() {
        Gauge.builder("outbox_events", metricsService, OutboxMetricsService::count)
                .description("Total number of outbox events")
                .register(registry);

        Arrays.stream(statuses).forEach(status ->
                        Gauge.builder("outbox_events_by_status", metricsService,
                                        m -> metricsService.countByStatus(status))
                                .description("Number of outbox events by status")
                                .tag("status", status.name().toLowerCase())
                                .register(registry)
                );

        properties.getEventHolders().keySet().forEach(type ->
                Arrays.stream(statuses)
                        .forEach(status ->
                                Gauge.builder("outbox_events_by_event_type_and_status", metricsService,
                                                m -> metricsService.countByEventTypeAndStatus(type, status))
                                        .description("Number of outbox events by type and status")
                                        .tags("event_type", type, "status", status.name().toLowerCase())
                                        .register(registry)
                        )
        );
    }
}