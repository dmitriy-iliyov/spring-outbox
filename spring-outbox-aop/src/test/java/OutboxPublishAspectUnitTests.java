
import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublish;
import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.aop.RowOutboxEvent;
import io.github.dmitriyiliyov.springoutbox.aop.RowOutboxEvents;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublishAspectUnitTests {

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    JoinPoint joinPoint;

    @Mock
    MethodSignature signature;

    @Mock
    OutboxPublish outboxPublish;

    OutboxPublishAspect tested;

    @BeforeEach
    void setUp() {
        tested = new OutboxPublishAspect(eventPublisher);
    }

    @Test
    @DisplayName("UT advice() when payload is default (#result) should publish RowOutboxEvent")
    void advice_whenPayloadDefault_shouldPublishRowOutboxEvent() {
        // given
        Object result = new Object();
        when(outboxPublish.payload()).thenReturn("#result");
        when(outboxPublish.eventType()).thenReturn("test-event");

        // when
        tested.advice(joinPoint, outboxPublish, result);

        // then
        ArgumentCaptor<RowOutboxEvent> captor = ArgumentCaptor.forClass(RowOutboxEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("test-event", captor.getValue().eventType());
        assertEquals(result, captor.getValue().event());
    }

    @Test
    @DisplayName("UT advice() when payload is null should publish RowOutboxEvent with result")
    void advice_whenPayloadNull_shouldPublishRowOutboxEventWithResult() {
        // given
        Object result = new Object();
        when(outboxPublish.payload()).thenReturn(null);
        when(outboxPublish.eventType()).thenReturn("test-event");

        // when
        tested.advice(joinPoint, outboxPublish, result);

        // then
        ArgumentCaptor<RowOutboxEvent> captor = ArgumentCaptor.forClass(RowOutboxEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("test-event", captor.getValue().eventType());
        assertEquals(result, captor.getValue().event());
    }

    @Test
    @DisplayName("UT advice() when payload is blank should publish RowOutboxEvent with result")
    void advice_whenPayloadBlank_shouldPublishRowOutboxEventWithResult() {
        // given
        Object result = new Object();
        when(outboxPublish.payload()).thenReturn("  ");
        when(outboxPublish.eventType()).thenReturn("test-event");

        // when
        tested.advice(joinPoint, outboxPublish, result);

        // then
        ArgumentCaptor<RowOutboxEvent> captor = ArgumentCaptor.forClass(RowOutboxEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("test-event", captor.getValue().eventType());
        assertEquals(result, captor.getValue().event());
    }

    @Test
    @DisplayName("UT advice() when payload is SpEL should evaluate and publish")
    void advice_whenPayloadIsSpel_shouldEvaluateAndPublish() {
        // given
        TestDto result = new TestDto("value");
        when(outboxPublish.payload()).thenReturn("#result.field");
        when(outboxPublish.eventType()).thenReturn("test-event");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        // when
        tested.advice(joinPoint, outboxPublish, result);

        // then
        ArgumentCaptor<RowOutboxEvent> captor = ArgumentCaptor.forClass(RowOutboxEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("test-event", captor.getValue().eventType());
        assertEquals("value", captor.getValue().event());
    }

    @Test
    @DisplayName("UT advice() when result is List should publish RowOutboxEvents")
    void advice_whenResultIsList_shouldPublishRowOutboxEvents() {
        // given
        List<Object> result = List.of(new Object(), new Object());
        when(outboxPublish.payload()).thenReturn("#result");
        when(outboxPublish.eventType()).thenReturn("test-event");

        // when
        tested.advice(joinPoint, outboxPublish, result);

        // then
        ArgumentCaptor<RowOutboxEvents> captor = ArgumentCaptor.forClass(RowOutboxEvents.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("test-event", captor.getValue().eventType());
        assertEquals(result, captor.getValue().events());
    }

    @Test
    @DisplayName("UT advice() when payload is null after evaluation should throw NPE")
    void advice_whenPayloadEvaluatesToNull_shouldThrowNPE() {
        // given
        TestDto result = new TestDto(null);
        when(outboxPublish.payload()).thenReturn("#result.field");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[]{});
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        // when + then
        assertThrows(NullPointerException.class, () -> tested.advice(joinPoint, outboxPublish, result));
    }

    record TestDto(String field) {}
}
