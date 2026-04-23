package io.github.dmitriyiliyov.springoutbox.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdaptiveOutboxScheduleStrategyIntegrationTests {

    @Mock
    private OutboxPublisherPropertiesHolder.EventPropertiesHolder properties;

    private ScheduledExecutorService realExecutor;
    private ScheduledExecutorService spyExecutor;

    private final List<Long> scheduledDelays = new CopyOnWriteArrayList<>();

    private static final long   INITIAL_DELAY_MS = 0L;
    private static final long   MIN_DELAY_MS     = 50L;
    private static final long   MAX_DELAY_MS     = 400L;
    private static final double MULTIPLIER       = 2.0;

    @BeforeEach
    void setUp() {
        scheduledDelays.clear();
        realExecutor = Executors.newScheduledThreadPool(2);
        spyExecutor  = spy(realExecutor);

        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(INITIAL_DELAY_MS));
        when(properties.getMinFixedDelay()).thenReturn(Duration.ofMillis(MIN_DELAY_MS));
        when(properties.getMaxFixedDelay()).thenReturn(Duration.ofMillis(MAX_DELAY_MS));
        when(properties.getMultiplier()).thenReturn(MULTIPLIER);

        doAnswer(inv -> {
            scheduledDelays.add(inv.getArgument(1, Long.class));
            return inv.callRealMethod();
        }).when(spyExecutor).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        realExecutor.shutdownNow();
        realExecutor.awaitTermination(1, TimeUnit.SECONDS);
    }

    private AdaptiveOutboxScheduleStrategy createStrategy() {
        return new AdaptiveOutboxScheduleStrategy(properties, spyExecutor);
    }

    @Test
    @DisplayName("IT adaptiveSchedule() when task has no work should produce strictly growing scheduled delays")
    void adaptiveSchedule_whenTaskHasNoWork_shouldProduceGrowingScheduledDelays() throws InterruptedException {
        var strategy = createStrategy();
        int targetExecutions = 4;
        CountDownLatch executionLatch = new CountDownLatch(targetExecutions);

        strategy.scheduleExecution(() -> {
            executionLatch.countDown();
            return false;
        });

        assertThat(executionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(150);

        assertThat(scheduledDelays).hasSizeGreaterThanOrEqualTo(targetExecutions + 1);
        assertThat(scheduledDelays.get(1)).isLessThan(scheduledDelays.get(2));
        assertThat(scheduledDelays.get(2)).isLessThan(scheduledDelays.get(3));
    }

    @Test
    @DisplayName("IT adaptiveSchedule() when task has no work should not exceed max delay")
    void adaptiveSchedule_whenTaskHasNoWork_shouldNotExceedMaxDelay() throws InterruptedException {
        var strategy = createStrategy();
        int targetExecutions = 8;
        CountDownLatch executionLatch = new CountDownLatch(targetExecutions);

        strategy.scheduleExecution(() -> {
            executionLatch.countDown();
            return false;
        });

        assertThat(executionLatch.await(15, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(150);

        assertThat(scheduledDelays).allSatisfy(
                delay -> assertThat(delay).isLessThanOrEqualTo(MAX_DELAY_MS)
        );
    }

    @Test
    @DisplayName("IT adaptiveSchedule() when task finds work after backoff should reset scheduled delay to min")
    void adaptiveSchedule_whenTaskFindsWorkAfterBackoff_shouldResetScheduledDelayToMin() throws InterruptedException {
        var strategy = createStrategy();
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch executionLatch = new CountDownLatch(4);

        strategy.scheduleExecution(() -> {
            boolean hasWork = executionCount.incrementAndGet() >= 4;
            executionLatch.countDown();
            return hasWork;
        });

        assertThat(executionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(150);

        assertThat(scheduledDelays).hasSizeGreaterThanOrEqualTo(5);
        assertThat(scheduledDelays.get(4)).isEqualTo(MIN_DELAY_MS);
    }

    @Test
    @DisplayName("IT adaptiveSchedule() full adaptive cycle should produce correct delay sequence")
    void adaptiveSchedule_fullAdaptiveCycle_shouldProduceCorrectDelaySequence() throws InterruptedException {
        var strategy = createStrategy();
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch executionLatch = new CountDownLatch(5);

        strategy.scheduleExecution(() -> {
            boolean hasWork = executionCount.incrementAndGet() >= 4;
            executionLatch.countDown();
            return hasWork;
        });

        assertThat(executionLatch.await(10, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(150);

        assertThat(scheduledDelays).hasSizeGreaterThanOrEqualTo(6);
        assertThat(scheduledDelays.get(0)).isEqualTo(INITIAL_DELAY_MS);
        assertThat(scheduledDelays.get(1)).isEqualTo(100L);
        assertThat(scheduledDelays.get(2)).isEqualTo(200L);
        assertThat(scheduledDelays.get(3)).isEqualTo(400L);
        assertThat(scheduledDelays.get(4)).isEqualTo(MIN_DELAY_MS);
        assertThat(scheduledDelays.get(5)).isEqualTo(MIN_DELAY_MS);
    }
}