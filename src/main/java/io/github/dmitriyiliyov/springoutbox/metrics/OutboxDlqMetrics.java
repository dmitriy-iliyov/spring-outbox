package io.github.dmitriyiliyov.springoutbox.metrics;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.core.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.dlq.OutboxDlqManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;

public class OutboxDlqMetrics implements OutboxMetrics {

    private final MeterRegistry registry;
    private final OutboxProperties properties;
    private final OutboxDlqManager manager;

    public OutboxDlqMetrics(MeterRegistry registry, OutboxProperties properties, OutboxDlqManager manager) {
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
