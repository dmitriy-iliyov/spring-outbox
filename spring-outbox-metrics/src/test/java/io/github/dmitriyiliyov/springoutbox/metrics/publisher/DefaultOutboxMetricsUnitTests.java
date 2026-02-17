package io.github.dmitriyiliyov.springoutbox.metrics.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxMetricsUnitTests {

    @Mock
    OutboxMetricsService metricsService;

    @Mock
    OutboxPublisherPropertiesHolder properties;

    SimpleMeterRegistry registry;

    DefaultOutboxMetrics tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Map<String, OutboxPublisherPropertiesHolder.EventPropertiesHolder> eventProps = Map.of(
                "test-event", Mockito.mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class)
        );
        Mockito.when(properties.getEventHolders()).thenReturn(eventProps);
        tested = new DefaultOutboxMetrics(properties, registry, metricsService);
    }

    @Test
    @DisplayName("UT register() should register total events gauge")
    void register_shouldRegisterTotalEventsGauge() {
        // given
        Mockito.when(metricsService.count()).thenReturn(10L);

        // when
        tested.register();

        // then
        Gauge gauge = registry.find("outbox_events").gauge();
        Assertions.assertNotNull(gauge);
        Assertions.assertEquals(10.0, gauge.value());
    }

    @Test
    @DisplayName("UT register() should register events by status gauges")
    void register_shouldRegisterEventsByStatusGauges() {
        // given
        Mockito.when(metricsService.countByStatus(EventStatus.PENDING)).thenReturn(5L);
        Mockito.when(metricsService.countByStatus(EventStatus.IN_PROCESS)).thenReturn(3L);

        // when
        tested.register();

        // then
        Gauge pendingGauge = registry.find("outbox_events_by_status")
                .tag("status", "pending")
                .gauge();
        Assertions.assertNotNull(pendingGauge);
        Assertions.assertEquals(5.0, pendingGauge.value());

        Gauge inProcessGauge = registry.find("outbox_events_by_status")
                .tag("status", "in_process")
                .gauge();
        Assertions.assertNotNull(inProcessGauge);
        Assertions.assertEquals(3.0, inProcessGauge.value());
    }

    @Test
    @DisplayName("UT register() should register events by type and status gauges")
    void register_shouldRegisterEventsByTypeAndStatusGauges() {
        // given
        Mockito.when(metricsService.countByEventTypeAndStatus("test-event", EventStatus.PENDING)).thenReturn(2L);
        Mockito.when(metricsService.countByEventTypeAndStatus("test-event", EventStatus.IN_PROCESS)).thenReturn(1L);

        // when
        tested.register();

        // then
        Gauge pendingGauge = registry.find("outbox_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "pending")
                .gauge();
        Assertions.assertNotNull(pendingGauge);
        Assertions.assertEquals(2.0, pendingGauge.value());

        Gauge inProcessGauge = registry.find("outbox_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "in_process")
                .gauge();
        Assertions.assertNotNull(inProcessGauge);
        Assertions.assertEquals(1.0, inProcessGauge.value());
    }
}
