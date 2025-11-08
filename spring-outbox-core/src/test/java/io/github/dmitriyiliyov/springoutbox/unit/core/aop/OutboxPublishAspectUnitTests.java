package io.github.dmitriyiliyov.springoutbox.unit.core.aop;

import io.github.dmitriyiliyov.springoutbox.publisher.core.aop.OutboxPublish;
import io.github.dmitriyiliyov.springoutbox.publisher.core.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.publisher.core.aop.RowOutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.core.aop.RowOutboxEvents;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxPublishAspectUnitTests {

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    OutboxPublishAspect tested;

    @Test
    @DisplayName("UT publishEvent(), payload = result, single object, should publish RowOutboxEvent with correct payload")
    void publishEvent_whenPayloadIsResultSingle_shouldAdvice() {
        // given
        JoinPoint joinPoint = mockJoinPointWithEmptyArgs();
        OutboxPublish outboxPublish = mock(OutboxPublish.class);
        when(outboxPublish.payload()).thenReturn("#result");
        when(outboxPublish.eventType()).thenReturn("EVENT");
        String result = "payload";

        // when
        tested.advice(joinPoint, outboxPublish, result);

        // then
        ArgumentCaptor<RowOutboxEvent> captor = ArgumentCaptor.forClass(RowOutboxEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        RowOutboxEvent event = captor.getValue();
        assertEquals("EVENT", event.eventType());
        assertEquals(result, event.event());
    }

    @Test
    @DisplayName("UT publishEvent(), payload = result, list, should publish RowOutboxEvents with correct payload")
    void publishEvent_whenPayloadIsResultList_shouldPublishRowOutboxEvents() {
        // given
        JoinPoint joinPoint = mockJoinPointWithEmptyArgs();
        OutboxPublish outboxPublish = mock(OutboxPublish.class);
        when(outboxPublish.payload()).thenReturn("#result");
        when(outboxPublish.eventType()).thenReturn("EVENT");
        List<String> result = List.of("a", "b");

        // when
        tested.advice(joinPoint, outboxPublish, result);

        // then
        ArgumentCaptor<RowOutboxEvents> captor = ArgumentCaptor.forClass(RowOutboxEvents.class);
        verify(eventPublisher).publishEvent(captor.capture());
        RowOutboxEvents event = captor.getValue();
        assertEquals("EVENT", event.eventType());
        assertEquals(result, event.events());
    }

    @Test
    @DisplayName("UT publishEvent(), payload via SpEL on method arg, should publish RowOutboxEvent with correct payload")
    void publishEvent_whenPayloadViaSpELOnArg_shouldAdvice() {
        // given
        JoinPoint joinPoint = mockJoinPointWithArgs(new String[]{"param"}, new Object[]{"value"});
        OutboxPublish outboxPublish = mock(OutboxPublish.class);
        when(outboxPublish.payload()).thenReturn("#param");
        when(outboxPublish.eventType()).thenReturn("EVENT");

        // when
        tested.advice(joinPoint, outboxPublish, null);

        // then
        ArgumentCaptor<RowOutboxEvent> captor = ArgumentCaptor.forClass(RowOutboxEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        RowOutboxEvent event = captor.getValue();
        assertEquals("EVENT", event.eventType());
        assertEquals("value", event.event());
    }

    @Test
    @DisplayName("UT publishEvent(), payload via SpEL returns list, should publish RowOutboxEvents with correct payload")
    void publishEvent_whenPayloadViaSpELList_shouldPublishRowOutboxEvents() {
        // given
        JoinPoint joinPoint = mockJoinPointWithArgs(new String[]{"param"}, new Object[]{List.of("x", "y")});
        OutboxPublish outboxPublish = mock(OutboxPublish.class);
        when(outboxPublish.payload()).thenReturn("#param");
        when(outboxPublish.eventType()).thenReturn("EVENT");

        // when
        tested.advice(joinPoint, outboxPublish, null);

        // then
        ArgumentCaptor<RowOutboxEvents> captor = ArgumentCaptor.forClass(RowOutboxEvents.class);
        verify(eventPublisher).publishEvent(captor.capture());
        RowOutboxEvents event = captor.getValue();
        assertEquals("EVENT", event.eventType());
        assertEquals(List.of("x", "y"), event.events());
    }

    @Test
    @DisplayName("UT publishEvent(), null result should throw NullPointerException")
    void advice_whenResultIsNull_shouldThrowNPE() {
        // given
        JoinPoint joinPoint = mockJoinPointWithEmptyArgs();
        OutboxPublish outboxPublish = mock(OutboxPublish.class);

        // when + then
        assertThrows(NullPointerException.class, () -> tested.advice(joinPoint, outboxPublish, null));
    }

    @Test
    @DisplayName("UT publishEvent(), blank SpEL should use result directly")
    void advice_whenBlankSpEL_shouldUseResult() {
        // given
        JoinPoint joinPoint = mockJoinPointWithEmptyArgs();
        OutboxPublish outboxPublish = mock(OutboxPublish.class);
        when(outboxPublish.payload()).thenReturn("  ");
        when(outboxPublish.eventType()).thenReturn("EVENT");
        String result = "payload";

        // when
        tested.advice(joinPoint, outboxPublish, result);

        // then
        ArgumentCaptor<RowOutboxEvent> captor = ArgumentCaptor.forClass(RowOutboxEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        RowOutboxEvent event = captor.getValue();
        assertEquals("EVENT", event.eventType());
        assertEquals(result, event.event());
    }

    private JoinPoint mockJoinPointWithArgs(String[] paramNames, Object[] args) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private JoinPoint mockJoinPointWithEmptyArgs() {
        return mockJoinPointWithArgs(null, new Object[]{});
    }
}
