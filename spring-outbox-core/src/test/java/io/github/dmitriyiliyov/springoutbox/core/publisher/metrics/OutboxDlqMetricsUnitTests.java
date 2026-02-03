package io.github.dmitriyiliyov.springoutbox.core.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqManager;
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
class OutboxDlqMetricsUnitTests {

    @Mock
    OutboxDlqManager manager;

    @Mock
    OutboxPublisherPropertiesHolder properties;

    SimpleMeterRegistry registry;
    OutboxDlqMetrics tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Map<String, OutboxPublisherPropertiesHolder.EventPropertiesHolder> eventProps = Map.of(
                "test-event", mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class)
        );
        when(properties.getEventHolders()).thenReturn(eventProps);
        tested = new OutboxDlqMetrics(registry, properties, manager);
    }

    @Test
    @DisplayName("UT register() should register total events gauge")
    void register_shouldRegisterTotalEventsGauge() {
        // given
        when(manager.count()).thenReturn(15L);

        // when
        tested.register();

        // then
        Gauge gauge = registry.find("outbox_dlq_events").gauge();
        assertNotNull(gauge);
        assertEquals(15.0, gauge.value());
    }

    @Test
    @DisplayName("UT register() should register events by status gauges")
    void register_shouldRegisterEventsByStatusGauges() {
        // given
        when(manager.countByStatus(DlqStatus.MOVED)).thenReturn(7L);
        when(manager.countByStatus(DlqStatus.IN_PROCESS)).thenReturn(4L);
        when(manager.countByStatus(DlqStatus.TO_RETRY)).thenReturn(2L);

        // when
        tested.register();

        // then
        Gauge movedGauge = registry.find("outbox_dlq_events_by_status")
                .tag("status", "moved")
                .gauge();
        assertNotNull(movedGauge);
        assertEquals(7.0, movedGauge.value());

        Gauge inProcessGauge = registry.find("outbox_dlq_events_by_status")
                .tag("status", "in_process")
                .gauge();
        assertNotNull(inProcessGauge);
        assertEquals(4.0, inProcessGauge.value());

        Gauge toRetryGauge = registry.find("outbox_dlq_events_by_status")
                .tag("status", "to_retry")
                .gauge();
        assertNotNull(toRetryGauge);
        assertEquals(2.0, toRetryGauge.value());
    }

    @Test
    @DisplayName("UT register() should register events by type and status gauges")
    void register_shouldRegisterEventsByTypeAndStatusGauges() {
        // given
        when(manager.countByEventTypeAndStatus("test-event", DlqStatus.MOVED)).thenReturn(3L);
        when(manager.countByEventTypeAndStatus("test-event", DlqStatus.IN_PROCESS)).thenReturn(2L);
        when(manager.countByEventTypeAndStatus("test-event", DlqStatus.TO_RETRY)).thenReturn(1L);

        // when
        tested.register();

        // then
        Gauge movedGauge = registry.find("outbox_dlq_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "moved")
                .gauge();
        assertNotNull(movedGauge);
        assertEquals(3.0, movedGauge.value());

        Gauge inProcessGauge = registry.find("outbox_dlq_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "in_process")
                .gauge();
        assertNotNull(inProcessGauge);
        assertEquals(2.0, inProcessGauge.value());

        Gauge toRetryGauge = registry.find("outbox_dlq_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "to_retry")
                .gauge();
        assertNotNull(toRetryGauge);
        assertEquals(1.0, toRetryGauge.value());
    }
}
