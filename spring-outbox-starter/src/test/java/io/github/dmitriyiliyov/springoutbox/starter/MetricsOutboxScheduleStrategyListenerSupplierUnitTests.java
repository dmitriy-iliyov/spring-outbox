package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategyListener;
import io.github.dmitriyiliyov.springoutbox.metrics.MetricsOutboxScheduleStrategyListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsOutboxScheduleStrategyListenerSupplierUnitTests {

    SimpleMeterRegistry registry;
    MetricsOutboxScheduleStrategyListenerSupplier tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        tested = new MetricsOutboxScheduleStrategyListenerSupplier(registry);
    }

    @Test
    @DisplayName("UT supply() should return MetricsOutboxScheduleStrategyListener instance")
    void supply_shouldReturnMetricsOutboxScheduleStrategyListenerInstance() {
        OutboxScheduleStrategyListener listener = tested.supply("test_task");

        assertInstanceOf(MetricsOutboxScheduleStrategyListener.class, listener);
    }

    @Test
    @DisplayName("UT supply() should return new instance on each call")
    void supply_shouldReturnNewInstanceOnEachCall() {
        OutboxScheduleStrategyListener first  = tested.supply("test_task");
        OutboxScheduleStrategyListener second = tested.supply("test_task");

        assertNotSame(first, second);
    }

    @Test
    @DisplayName("UT supply() returned listener should register metrics with provided task type tag")
    void supply_returnedListener_shouldRegisterMetricsWithCorrectTag() {
        OutboxScheduleStrategyListener listener = tested.supply("recovery_task");

        listener.onExecutionStarted();

        Counter counter = registry.find("outbox_started_tasks")
                .tag("task_type", "recovery_task")
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    @DisplayName("UT supply() listeners with different task types should register independent metrics")
    void supply_differentTaskTypes_shouldRegisterIndependentMetrics() {
        OutboxScheduleStrategyListener first  = tested.supply("task_a");
        OutboxScheduleStrategyListener second = tested.supply("task_b");

        first.onExecutionStarted();
        first.onExecutionStarted();
        second.onExecutionStarted();

        assertEquals(2, registry.find("outbox_started_tasks")
                .tag("task_type", "task_a").counter().count());
        assertEquals(1, registry.find("outbox_started_tasks")
                .tag("task_type", "task_b").counter().count());
    }
}