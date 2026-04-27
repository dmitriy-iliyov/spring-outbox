package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.AdaptiveOutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.FixedOutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class OutboxScheduleStrategyFactoryUnitTests {

    @Test
    @DisplayName("UT create() should return FixedOutboxScheduleStrategy when type is FIXED")
    public void create_whenTypeIsFixed_shouldReturnFixedStrategy() {
        // given
        OutboxProperties.PollingProperties properties = new OutboxProperties.PollingProperties();
        properties.setType(PollingType.FIXED);
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);

        // when
        OutboxScheduleStrategy strategy = OutboxScheduleStrategyFactory.create("taskType", properties, executorMock, mock(OutboxScheduleStrategyListenerSupplier.class));

        // then
        assertNotNull(strategy);
        assertTrue(strategy instanceof FixedOutboxScheduleStrategy,
                "Strategy should be instance of FixedOutboxScheduleStrategy");
    }

    @Test
    @DisplayName("UT create() should return AdaptiveOutboxScheduleStrategy when type is ADAPTIVE")
    public void create_whenTypeIsAdaptive_shouldReturnAdaptiveStrategy() {
        // given
        OutboxProperties.PollingProperties properties = new OutboxProperties.PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofMinutes(1));
        properties.setMinFixedDelay(Duration.ofSeconds(1));
        properties.setMaxFixedDelay(Duration.ofSeconds(10));
        properties.setMultiplier(2.0);
        
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);

        // when
        OutboxScheduleStrategy strategy = OutboxScheduleStrategyFactory.create("taskType", properties, executorMock, mock(OutboxScheduleStrategyListenerSupplier.class));

        // then
        assertNotNull(strategy);
        assertTrue(strategy instanceof AdaptiveOutboxScheduleStrategy,
                "Strategy should be instance of AdaptiveOutboxScheduleStrategy");
    }

    @Test
    @DisplayName("UT create() should throw IllegalStateException when type is null (unreachable branch)")
    public void create_whenTypeIsNull_shouldThrowException() {
        // given
        OutboxProperties.PollingProperties properties = new OutboxProperties.PollingProperties();
        properties.setType(null);
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);

        // when
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> OutboxScheduleStrategyFactory.create("taskType", properties, executorMock, mock(OutboxScheduleStrategyListenerSupplier.class)));

        // then
        assertEquals("Reached unreachable branch during creating OutboxScheduleStrategy", e.getMessage());
    }
}