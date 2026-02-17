package io.github.dmitriyiliyov.springoutbox.aop;

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
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    @DisplayName("UT getContext() should map parameters to variables when names and args match")
    void getContext_shouldMapParametersToVariables_whenNamesAndArgsMatch() {
        // given
        when(joinPoint.getSignature()).thenReturn(signature);
        String[] paramNames = {"userId", "productName"};
        Object[] args = {123L, "Laptop"};
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);

        // when
        StandardEvaluationContext context = tested.getContext(joinPoint);

        // then
        assertThat(context.lookupVariable("userId")).isEqualTo(123L);
        assertThat(context.lookupVariable("productName")).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("UT getContext() should use indexed names when parameter names are null")
    void getContext_shouldUseIndexedNames_whenParameterNamesAreNull() {
        // given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(null);
        Object[] args = {"someValue", 99};
        when(joinPoint.getArgs()).thenReturn(args);

        // when
        StandardEvaluationContext context = tested.getContext(joinPoint);

        // then
        assertThat(context.lookupVariable("args[0]")).isEqualTo("someValue");
        assertThat(context.lookupVariable("args[1]")).isEqualTo(99);
    }

    @Test
    @DisplayName("UT getContext() should return empty context when no args")
    void getContext_shouldReturnEmptyContext_whenNoArgs() {
        // given
        when(joinPoint.getSignature()).thenReturn(signature);
        String[] paramNames = new String[0];
        Object[] args = new Object[0];
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);

        // when
        StandardEvaluationContext context = tested.getContext(joinPoint);

        // then
        assertThat(context.lookupVariable("anyVar")).isNull();
    }

    @Test
    @DisplayName("UT getContext() should use indexed names for args exceeding param names")
    void getContext_shouldUseIndexedNames_forArgsExceedingParamNames() {
        // given
        when(joinPoint.getSignature()).thenReturn(signature);
        String[] paramNames = {"first"};
        Object[] args = {"firstValue", "secondValue"};
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);

        // when
        StandardEvaluationContext context = tested.getContext(joinPoint);

        // then
        assertThat(context.lookupVariable("first")).isEqualTo("firstValue");
        assertThat(context.lookupVariable("args[1]")).isEqualTo("secondValue");
    }

    record TestDto(String field) {}
}
