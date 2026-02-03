package io.github.dmitriyiliyov.springoutbox.core.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.core.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;

public final class OutboxDlqMetrics implements OutboxMetrics {

    private final MeterRegistry registry;
    private final OutboxPublisherPropertiesHolder properties;
    private final OutboxDlqManager manager;
    private final DlqStatus [] statuses = new DlqStatus[] {DlqStatus.MOVED, DlqStatus.IN_PROCESS, DlqStatus.TO_RETRY};

    public OutboxDlqMetrics(MeterRegistry registry, OutboxPublisherPropertiesHolder properties, OutboxDlqManager manager) {
        this.registry = registry;
        this.properties = properties;
        this.manager = manager;
    }

    @Override
    public void register() {
        Gauge.builder("outbox_dlq_events", manager, OutboxDlqManager::count)
                .description("Total number of outbox DLQ events")
                .register(registry);

        Arrays.stream(statuses).forEach(status ->
                        Gauge.builder("outbox_dlq_events_by_status", manager, m -> manager.countByStatus(status))
                                .description("Number of outbox DLQ events by status")
                                .tag("status", status.name().toLowerCase())
                                .register(registry)
                );

        properties.getEventHolders().keySet().forEach(type ->
                Arrays.stream((statuses)).forEach(status ->
                                Gauge.builder("outbox_dlq_events_by_event_type_and_status", manager,
                                                m -> manager.countByEventTypeAndStatus(type, status))
                                        .description("Number of outbox DLQ events by type and status")
                                        .tags("event_type", type, "status", status.name().toLowerCase())
                                        .register(registry)
                        )
        );
    }
}
