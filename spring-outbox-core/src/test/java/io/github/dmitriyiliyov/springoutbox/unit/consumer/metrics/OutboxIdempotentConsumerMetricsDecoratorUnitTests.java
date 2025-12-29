package io.github.dmitriyiliyov.springoutbox.unit.consumer.metrics;

import io.github.dmitriyiliyov.springoutbox.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.consumer.metrics.OutboxIdempotentConsumerMetricsDecorator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxIdempotentConsumerMetricsDecoratorUnitTests {

    @Mock
    private OutboxIdempotentConsumer delegate;

    @Mock
    private MeterRegistry registry;

    @Mock
    private Counter failsCounter;

    private OutboxIdempotentConsumerMetricsDecorator decorator;

    @BeforeEach
    void setUp() {
        when(registry.counter(eq("consumed_outbox_events_total"), eq("type"), eq("failed")))
                .thenReturn(failsCounter);

        decorator = new OutboxIdempotentConsumerMetricsDecorator(delegate, registry);
    }

    @Test
    @DisplayName("UT consume() when operation succeeds should not increment fails counter")
    void consume_whenOperationSucceeds_shouldNotIncrementFailsCounter() {
        // given
        String message = "test-message";
        Runnable operation = mock(Runnable.class);
        doNothing().when(delegate).consume(anyString(), any(Runnable.class));

        // when
        decorator.consume(message, operation);

        // then
        verify(delegate).consume(anyString(), any(Runnable.class));
        verify(failsCounter, never()).increment();
    }

    @Test
    @DisplayName("UT consume() when operation fails should increment fails counter and rethrow exception")
    void consume_whenOperationFails_shouldIncrementFailsCounterAndRethrowException() {
        // given
        String message = "test-message";
        Runnable operation = mock(Runnable.class);
        RuntimeException exception = new RuntimeException("Operation failed");
        doThrow(exception).when(delegate).consume(anyString(), any(Runnable.class));

        // when + then
        assertThatThrownBy(() -> decorator.consume(message, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Operation failed");
        verify(delegate).consume(anyString(), any(Runnable.class));
        verify(failsCounter).increment();
    }

    @Test
    @DisplayName("UT consume() when delegate throws exception should increment fails counter")
    void consume_whenDelegateThrowsException_shouldIncrementFailsCounter() {
        // given
        String message = "test-message";
        Runnable operation = mock(Runnable.class);
        IllegalStateException exception = new IllegalStateException("Delegate exception");
        doThrow(exception).when(delegate).consume(anyString(), any(Runnable.class));

        // when + then
        assertThatThrownBy(() -> decorator.consume(message, operation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Delegate exception");
        verify(delegate).consume(anyString(), any(Runnable.class));
        verify(failsCounter).increment();
    }

    @Test
    @DisplayName("UT consume() with null message when operation succeeds should not increment fails counter")
    void consume_withNullMessage_whenOperationSucceeds_shouldNotIncrementFailsCounter() {
        // given
        String message = null;
        Runnable operation = mock(Runnable.class);
        doNothing().when(delegate).consume(isNull(), any(Runnable.class));

        // when
        decorator.consume(message, operation);

        // then
        verify(delegate).consume(isNull(), any(Runnable.class));
        verify(failsCounter, never()).increment();
    }

    @Test
    @DisplayName("UT consume() batch when operation succeeds should not increment fails counter")
    void consumeBatch_whenOperationSucceeds_shouldNotIncrementFailsCounter() {
        // given
        List<String> messages = List.of("msg1", "msg2", "msg3");
        Consumer<List<String>> operation = mock(Consumer.class);
        doNothing().when(delegate).consume(anyList(), any(Consumer.class));

        // when
        decorator.consume(messages, operation);

        // then
        verify(delegate).consume(anyList(), any(Consumer.class));
        verify(failsCounter, never()).increment(anyDouble());
    }

    @Test
    @DisplayName("UT consume() batch when operation fails should increment fails counter with batch size")
    void consumeBatch_whenOperationFails_shouldIncrementFailsCounterWithBatchSize() {
        // given
        List<String> messages = List.of("msg1", "msg2", "msg3");
        Consumer<List<String>> operation = mock(Consumer.class);
        RuntimeException exception = new RuntimeException("Batch operation failed");
        doThrow(exception).when(delegate).consume(anyList(), any(Consumer.class));

        // when + then
        assertThatThrownBy(() -> decorator.consume(messages, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Batch operation failed");
        verify(delegate).consume(anyList(), any(Consumer.class));
        verify(failsCounter).increment(3.0);
    }

    @Test
    @DisplayName("UT consume() batch with single message when operation fails should increment fails counter by one")
    void consumeBatch_withSingleMessage_whenOperationFails_shouldIncrementFailsCounterByOne() {
        // given
        List<String> messages = List.of("msg1");
        Consumer<List<String>> operation = mock(Consumer.class);
        RuntimeException exception = new RuntimeException("Single message failed");
        doThrow(exception).when(delegate).consume(anyList(), any(Consumer.class));

        // when + then
        assertThatThrownBy(() -> decorator.consume(messages, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Single message failed");
        verify(delegate).consume(anyList(), any(Consumer.class));
        verify(failsCounter).increment(1.0);
    }

    @Test
    @DisplayName("UT consume() batch with empty list when operation fails should increment fails counter with zero")
    void consumeBatch_withEmptyList_whenOperationFails_shouldIncrementFailsCounterWithZero() {
        // given
        List<String> messages = List.of();
        Consumer<List<String>> operation = mock(Consumer.class);
        RuntimeException exception = new RuntimeException("Empty batch failed");
        doThrow(exception).when(delegate).consume(anyList(), any(Consumer.class));

        // when + then
        assertThatThrownBy(() -> decorator.consume(messages, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Empty batch failed");
        verify(delegate).consume(anyList(), any(Consumer.class));
        verify(failsCounter).increment(0.0);
    }

    @Test
    @DisplayName("UT consume() batch when delegate throws exception should increment fails counter")
    void consumeBatch_whenDelegateThrowsException_shouldIncrementFailsCounter() {
        // given
        List<String> messages = List.of("msg1", "msg2");
        Consumer<List<String>> operation = mock(Consumer.class);
        IllegalStateException exception = new IllegalStateException("Delegate exception in batch");
        doThrow(exception).when(delegate).consume(anyList(), any(Consumer.class));

        // when + then
        assertThatThrownBy(() -> decorator.consume(messages, operation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Delegate exception in batch");
        verify(delegate).consume(anyList(), any(Consumer.class));
        verify(failsCounter).increment(2.0);
    }
}