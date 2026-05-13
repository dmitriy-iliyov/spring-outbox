package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostApplicationReadyOutboxInitializerUnitTests {

    @Mock
    OutboxProperties properties;

    @Test
    @DisplayName("UT init() when schedulers present should call schedule on each")
    void init_whenSchedulersPresent_shouldCallScheduleOnEach() {
        // given
        OutboxScheduler scheduler1 = mock(OutboxScheduler.class);
        OutboxScheduler scheduler2 = mock(OutboxScheduler.class);

        PostApplicationReadyOutboxInitializer initializer = new PostApplicationReadyOutboxInitializer(
                properties, Map.of("scheduler1", scheduler1, "scheduler2", scheduler2), Collections.emptyMap()
        );

        // when
        initializer.init();

        // then
        verify(scheduler1).schedule();
        verify(scheduler2).schedule();
    }

    @Test
    @DisplayName("UT init() when no schedulers present should not throw")
    void init_whenNoSchedulers_shouldNotThrow() {
        // given
        PostApplicationReadyOutboxInitializer initializer = new PostApplicationReadyOutboxInitializer(
                properties, Collections.emptyMap(), Collections.emptyMap()
        );

        // when + then (no exception is thrown)
        initializer.init();
    }

    @Test
    @DisplayName("UT init() when metrics present should call register on each")
    void init_whenMetricsPresent_shouldCallRegisterOnEach() {
        // given
        OutboxMetrics metrics1 = mock(OutboxMetrics.class);
        OutboxMetrics metrics2 = mock(OutboxMetrics.class);

        PostApplicationReadyOutboxInitializer initializer = new PostApplicationReadyOutboxInitializer(
                properties, Collections.emptyMap(), Map.of("metrics1", metrics1, "metrics2", metrics2)
        );

        // when
        initializer.init();

        // then
        verify(metrics1).register();
        verify(metrics2).register();
    }
}