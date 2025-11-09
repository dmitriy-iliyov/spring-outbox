package io.github.dmitriyiliyov.springoutbox.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;

public final class DefaultOutboxMetrics implements OutboxMetrics {

    private final MeterRegistry registry;
    private final OutboxPublisherProperties properties;
    private final OutboxManager manager;

    public DefaultOutboxMetrics(MeterRegistry registry, OutboxPublisherProperties properties, OutboxManager manager) {
        this.registry = registry;
        this.properties = properties;
        this.manager = manager;
    }

    @Override
    public void register() {
        Gauge.builder("outbox_events_count", manager, OutboxManager::count)
                .description("Total number of outbox events")
                .register(registry);

        Arrays.stream(EventStatus.values())
                .forEach(status ->
                        Gauge.builder("outbox_events_count", manager, m -> manager.countByStatus(status))
                                .description("Number of outbox events by status")
                                .tag("status", status.name())
                                .register(registry)
                );

        properties.getEvents().keySet().forEach(type ->
                Arrays.stream(EventStatus.values())
                        .forEach(status ->
                                Gauge.builder("outbox_events_count", manager,
                                                m -> manager.countByEventTypeAndStatus(type, status))
                                        .description("Number of outbox events by type and status")
                                        .tags("eventType", type, "status", status.name())
                                        .register(registry)
                        )
        );
    }
}