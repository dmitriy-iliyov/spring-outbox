package io.github.dmitriyiliyov.springoutbox.core.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxMetricsUnitTests {

    @Mock
    OutboxManager manager;

    @Mock
    OutboxPublisherPropertiesHolder properties;

    SimpleMeterRegistry registry;
    DefaultOutboxMetrics tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Map<String, OutboxPublisherPropertiesHolder.EventPropertiesHolder> eventProps = Map.of(
                "test-event", mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class)
        );
        when(properties.getEventHolders()).thenReturn(eventProps);
        tested = new DefaultOutboxMetrics(registry, properties, manager);
    }

    @Test
    @DisplayName("UT register() should register total events gauge")
    void register_shouldRegisterTotalEventsGauge() {
        // given
        when(manager.count()).thenReturn(10L);

        // when
        tested.register();

        // then
        Gauge gauge = registry.find("outbox_events").gauge();
        assertNotNull(gauge);
        assertEquals(10.0, gauge.value());
    }

    @Test
    @DisplayName("UT register() should register events by status gauges")
    void register_shouldRegisterEventsByStatusGauges() {
        // given
        when(manager.countByStatus(EventStatus.PENDING)).thenReturn(5L);
        when(manager.countByStatus(EventStatus.IN_PROCESS)).thenReturn(3L);

        // when
        tested.register();

        // then
        Gauge pendingGauge = registry.find("outbox_events_by_status")
                .tag("status", "pending")
                .gauge();
        assertNotNull(pendingGauge);
        assertEquals(5.0, pendingGauge.value());

        Gauge inProcessGauge = registry.find("outbox_events_by_status")
                .tag("status", "in_process")
                .gauge();
        assertNotNull(inProcessGauge);
        assertEquals(3.0, inProcessGauge.value());
    }

    @Test
    @DisplayName("UT register() should register events by type and status gauges")
    void register_shouldRegisterEventsByTypeAndStatusGauges() {
        // given
        when(manager.countByEventTypeAndStatus("test-event", EventStatus.PENDING)).thenReturn(2L);
        when(manager.countByEventTypeAndStatus("test-event", EventStatus.IN_PROCESS)).thenReturn(1L);

        // when
        tested.register();

        // then
        Gauge pendingGauge = registry.find("outbox_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "pending")
                .gauge();
        assertNotNull(pendingGauge);
        assertEquals(2.0, pendingGauge.value());

        Gauge inProcessGauge = registry.find("outbox_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "in_process")
                .gauge();
        assertNotNull(inProcessGauge);
        assertEquals(1.0, inProcessGauge.value());
    }
}
