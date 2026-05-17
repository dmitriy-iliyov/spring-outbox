package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulersPostApplicationReadyOutboxInitializerUnitTests {

    @Test
    @DisplayName("UT init() when schedulers present should call schedule on each")
    void init_whenSchedulersPresent_shouldCallScheduleOnEach() {
        // given
        OutboxScheduler scheduler1 = mock(OutboxScheduler.class);
        OutboxScheduler scheduler2 = mock(OutboxScheduler.class);

        SchedulersPostApplicationReadyOutboxInitializer initializer = new SchedulersPostApplicationReadyOutboxInitializer(
                Map.of("scheduler1", scheduler1, "scheduler2", scheduler2)
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
        SchedulersPostApplicationReadyOutboxInitializer initializer = new SchedulersPostApplicationReadyOutboxInitializer(
                Collections.emptyMap()
        );

        // when + then (no exception is thrown)
        initializer.init();
    }
}