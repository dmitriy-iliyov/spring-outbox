package io.github.dmitriyiliyov.springoutbox.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OutboxDlqManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;

public final class OutboxDlqMetrics implements OutboxMetrics {

    private final MeterRegistry registry;
    private final OutboxPublisherProperties properties;
    private final OutboxDlqManager manager;

    public OutboxDlqMetrics(MeterRegistry registry, OutboxPublisherProperties properties, OutboxDlqManager manager) {
        this.registry = registry;
        this.properties = properties;
        this.manager = manager;
    }

    @Override
    public void register() {
        Gauge.builder("outbox_dlq_events_count", manager, OutboxDlqManager::count)
                .description("Total number of outbox DLQ events")
                .register(registry);

        Arrays.stream(DlqStatus.values())
                .forEach(status ->
                        Gauge.builder("outbox_dlq_events_count", manager, m -> manager.countByStatus(status))
                                .description("Number of outbox DLQ events by status")
                                .tag("status", status.name())
                                .register(registry)
                );

        properties.getEvents().keySet().forEach(type ->
                Arrays.stream(DlqStatus.values())
                        .forEach(status ->
                                Gauge.builder("outbox_dlq_events_count", manager,
                                                m -> manager.countByEventTypeAndStatus(type, status))
                                        .description("Number of outbox DLQ events by type and status")
                                        .tags("eventType", type, "status", status.name())
                                        .register(registry)
                        )
        );
    }
}
