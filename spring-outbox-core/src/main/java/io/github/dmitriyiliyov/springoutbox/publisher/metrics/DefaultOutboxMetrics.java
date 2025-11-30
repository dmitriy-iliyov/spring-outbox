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
    private final EventStatus [] statuses = new EventStatus[] {EventStatus.PENDING, EventStatus.IN_PROCESS};

    public DefaultOutboxMetrics(MeterRegistry registry, OutboxPublisherProperties properties, OutboxManager manager) {
        this.registry = registry;
        this.properties = properties;
        this.manager = manager;
    }

    @Override
    public void register() {
        Gauge.builder("outbox_events", manager, OutboxManager::count)
                .description("Total number of outbox events")
                .register(registry);

        Arrays.stream(statuses).forEach(status ->
                        Gauge.builder("outbox_events_by_status", manager, m -> manager.countByStatus(status))
                                .description("Number of outbox events by status")
                                .tag("status", status.name().toLowerCase())
                                .register(registry)
                );

        properties.getEvents().keySet().forEach(type ->
                Arrays.stream(statuses)
                        .forEach(status ->
                                Gauge.builder("outbox_events_by_event_type_and_status", manager,
                                                m -> manager.countByEventTypeAndStatus(type, status))
                                        .description("Number of outbox events by type and status")
                                        .tags("event_type", type, "status", status.name().toLowerCase())
                                        .register(registry)
                        )
        );
    }
}