package io.github.dmitriyiliyov.springoutbox.core.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.core.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;

public final class DefaultOutboxMetrics implements OutboxMetrics {

    private final MeterRegistry registry;
    private final OutboxPublisherPropertiesHolder properties;
    private final OutboxManager manager;
    private final EventStatus [] statuses = new EventStatus[] {EventStatus.PENDING, EventStatus.IN_PROCESS};

    public DefaultOutboxMetrics(MeterRegistry registry, OutboxPublisherPropertiesHolder properties, OutboxManager manager) {
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

        properties.getEventHolders().keySet().forEach(type ->
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