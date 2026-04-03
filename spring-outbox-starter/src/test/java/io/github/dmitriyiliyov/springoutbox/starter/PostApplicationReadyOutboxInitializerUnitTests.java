package io.github.dmitriyiliyov.springoutbox.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostApplicationReadyOutboxInitializerUnitTests {

    @Mock
    ApplicationContext context;

    PostApplicationReadyOutboxInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new PostApplicationReadyOutboxInitializer(context);
    }

    @Test
    @DisplayName("UT init() when no OutboxProperties in context should throw ISE")
    void init_whenNoOutboxProperties_shouldThrowISE() {
        // given
        when(context.getBeansOfType(OutboxScheduler.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxMetrics.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxProperties.class)).thenReturn(Map.of());
        when(context.getBeansOfType(ObjectMapper.class)).thenReturn(Map.of());

        // when + then
        assertThrows(IllegalStateException.class, () -> initializer.init());
    }

    @Test
    @DisplayName("UT init() when schedulers present should call schedule on each")
    void init_whenSchedulersPresent_shouldCallScheduleOnEach() {
        // given
        OutboxScheduler scheduler1 = mock(OutboxScheduler.class);
        OutboxScheduler scheduler2 = mock(OutboxScheduler.class);
        OutboxProperties properties = mock(OutboxProperties.class);

        when(context.getBeansOfType(OutboxScheduler.class)).thenReturn(Map.of(
                "scheduler1", scheduler1,
                "scheduler2", scheduler2
        ));
        when(context.getBeansOfType(OutboxMetrics.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxProperties.class)).thenReturn(Map.of("outboxProperties", properties));
        when(context.getBeansOfType(ObjectMapper.class)).thenReturn(Map.of());

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
        OutboxProperties properties = mock(OutboxProperties.class);

        when(context.getBeansOfType(OutboxScheduler.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxMetrics.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxProperties.class)).thenReturn(Map.of("outboxProperties", properties));
        when(context.getBeansOfType(ObjectMapper.class)).thenReturn(Map.of());

        // when + then
        initializer.init();
    }

    @Test
    @DisplayName("UT init() when metrics present should call register on each")
    void init_whenMetricsPresent_shouldCallRegisterOnEach() {
        // given
        OutboxMetrics metrics1 = mock(OutboxMetrics.class);
        OutboxMetrics metrics2 = mock(OutboxMetrics.class);
        OutboxProperties properties = mock(OutboxProperties.class);

        when(context.getBeansOfType(OutboxScheduler.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxMetrics.class)).thenReturn(Map.of(
                "metrics1", metrics1,
                "metrics2", metrics2
        ));
        when(context.getBeansOfType(OutboxProperties.class)).thenReturn(Map.of("outboxProperties", properties));
        when(context.getBeansOfType(ObjectMapper.class)).thenReturn(Map.of());

        // when
        initializer.init();

        // then
        verify(metrics1).register();
        verify(metrics2).register();
    }

    @Test
    @DisplayName("UT init() when ObjectMapper present should use it to log properties")
    void init_whenObjectMapperPresent_shouldUseMapperToLogProperties() throws Exception {
        // given
        OutboxProperties properties = mock(OutboxProperties.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        ObjectWriter writer = mock(ObjectWriter.class);

        when(mapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);
        when(writer.writeValueAsString(properties)).thenReturn("{}");

        when(context.getBeansOfType(OutboxScheduler.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxMetrics.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxProperties.class)).thenReturn(Map.of("outboxProperties", properties));
        when(context.getBeansOfType(ObjectMapper.class)).thenReturn(Map.of("objectMapper", mapper));

        // when
        initializer.init();

        // then
        verify(mapper).writerWithDefaultPrettyPrinter();
        verify(writer).writeValueAsString(properties);
    }

    @Test
    @DisplayName("UT init() when ObjectMapper throws should not rethrow exception")
    void init_whenObjectMapperThrows_shouldNotRethrow() throws Exception {
        // given
        OutboxProperties properties = mock(OutboxProperties.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        ObjectWriter writer = mock(ObjectWriter.class);

        when(mapper.writerWithDefaultPrettyPrinter()).thenReturn(writer);
        when(writer.writeValueAsString(any())).thenThrow(new JsonProcessingException("serialization error") {});

        when(context.getBeansOfType(OutboxScheduler.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxMetrics.class)).thenReturn(Map.of());
        when(context.getBeansOfType(OutboxProperties.class)).thenReturn(Map.of("outboxProperties", properties));
        when(context.getBeansOfType(ObjectMapper.class)).thenReturn(Map.of("objectMapper", mapper));

        // when + then
        initializer.init();
    }
}