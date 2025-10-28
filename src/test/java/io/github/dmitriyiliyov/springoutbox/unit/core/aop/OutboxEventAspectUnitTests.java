package io.github.dmitriyiliyov.springoutbox.unit.core.aop;

import io.github.dmitriyiliyov.springoutbox.core.aop.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.aop.OutboxEventAspect;
import io.github.dmitriyiliyov.springoutbox.core.aop.RowOutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.aop.RowOutboxEvents;
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
public class OutboxEventAspectUnitTests {

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    OutboxEventAspect tested;

    @Test
    @DisplayName("UT publishEvent(), payload = result, single object, should publish RowOutboxEvent with correct payload")
    void publishEvent_whenPayloadIsResultSingle_shouldPublishRowOutboxEvent() {
        // given
        JoinPoint joinPoint = mockJoinPointWithEmptyArgs();
        OutboxEvent outboxEvent = mock(OutboxEvent.class);
        when(outboxEvent.payload()).thenReturn("#result");
        when(outboxEvent.eventType()).thenReturn("EVENT");
        String result = "payload";

        // when
        tested.publishEvent(joinPoint, outboxEvent, result);

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
        OutboxEvent outboxEvent = mock(OutboxEvent.class);
        when(outboxEvent.payload()).thenReturn("#result");
        when(outboxEvent.eventType()).thenReturn("EVENT");
        List<String> result = List.of("a", "b");

        // when
        tested.publishEvent(joinPoint, outboxEvent, result);

        // then
        ArgumentCaptor<RowOutboxEvents> captor = ArgumentCaptor.forClass(RowOutboxEvents.class);
        verify(eventPublisher).publishEvent(captor.capture());
        RowOutboxEvents event = captor.getValue();
        assertEquals("EVENT", event.eventType());
        assertEquals(result, event.events());
    }

    @Test
    @DisplayName("UT publishEvent(), payload via SpEL on method arg, should publish RowOutboxEvent with correct payload")
    void publishEvent_whenPayloadViaSpELOnArg_shouldPublishRowOutboxEvent() {
        // given
        JoinPoint joinPoint = mockJoinPointWithArgs(new String[]{"param"}, new Object[]{"value"});
        OutboxEvent outboxEvent = mock(OutboxEvent.class);
        when(outboxEvent.payload()).thenReturn("#param");
        when(outboxEvent.eventType()).thenReturn("EVENT");

        // when
        tested.publishEvent(joinPoint, outboxEvent, null);

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
        OutboxEvent outboxEvent = mock(OutboxEvent.class);
        when(outboxEvent.payload()).thenReturn("#param");
        when(outboxEvent.eventType()).thenReturn("EVENT");

        // when
        tested.publishEvent(joinPoint, outboxEvent, null);

        // then
        ArgumentCaptor<RowOutboxEvents> captor = ArgumentCaptor.forClass(RowOutboxEvents.class);
        verify(eventPublisher).publishEvent(captor.capture());
        RowOutboxEvents event = captor.getValue();
        assertEquals("EVENT", event.eventType());
        assertEquals(List.of("x", "y"), event.events());
    }

    @Test
    @DisplayName("UT publishEvent(), null result should throw NullPointerException")
    void publishEvent_whenResultIsNull_shouldThrowNPE() {
        // given
        JoinPoint joinPoint = mockJoinPointWithEmptyArgs();
        OutboxEvent outboxEvent = mock(OutboxEvent.class);

        // when + then
        assertThrows(NullPointerException.class, () -> tested.publishEvent(joinPoint, outboxEvent, null));
    }

    @Test
    @DisplayName("UT publishEvent(), blank SpEL should use result directly")
    void publishEvent_whenBlankSpEL_shouldUseResult() {
        // given
        JoinPoint joinPoint = mockJoinPointWithEmptyArgs();
        OutboxEvent outboxEvent = mock(OutboxEvent.class);
        when(outboxEvent.payload()).thenReturn("  ");
        when(outboxEvent.eventType()).thenReturn("EVENT");
        String result = "payload";

        // when
        tested.publishEvent(joinPoint, outboxEvent, result);

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
