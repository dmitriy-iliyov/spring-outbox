package io.github.dmitriyiliyov.springoutbox.unit.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.consumer.DefaultOutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.consumer.OutboxEventIdResolveManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxIdempotentConsumerUnitTests {

    @Mock
    private OutboxEventIdResolveManager resolvingManager;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ConsumedOutboxManager consumedOutboxManager;

    private DefaultOutboxIdempotentConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DefaultOutboxIdempotentConsumer(
                resolvingManager,
                transactionTemplate,
                consumedOutboxManager
        );
    }

    @Test
    @DisplayName("UT consume() when event not consumed should execute operation")
    void consume_whenEventNotConsumed_shouldExecuteOperation() {
        // given
        UUID eventId = UUID.randomUUID();
        String message = "test-message";
        Runnable operation = mock(Runnable.class);

        when(resolvingManager.resolve(message)).thenReturn(eventId);
        when(consumedOutboxManager.isConsumed(eventId)).thenReturn(false);
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        consumer.consume(message, operation);

        // then
        verify(resolvingManager).resolve(message);
        verify(transactionTemplate).executeWithoutResult(any());
        verify(consumedOutboxManager).isConsumed(eventId);
        verify(operation).run();
    }

    @Test
    @DisplayName("UT consume() when event already consumed should not execute operation")
    void consume_whenEventAlreadyConsumed_shouldNotExecuteOperation() {
        // given
        UUID eventId = UUID.randomUUID();
        String message = "test-message";
        Runnable operation = mock(Runnable.class);

        when(resolvingManager.resolve(message)).thenReturn(eventId);
        when(consumedOutboxManager.isConsumed(eventId)).thenReturn(true);
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        consumer.consume(message, operation);

        // then
        verify(resolvingManager).resolve(message);
        verify(transactionTemplate).executeWithoutResult(any());
        verify(consumedOutboxManager).isConsumed(eventId);
        verify(operation, never()).run();
    }

    @Test
    @DisplayName("UT consume() when operation throws exception should rethrow")
    void consume_whenOperationThrowsException_shouldRethrow() {
        // given
        UUID eventId = UUID.randomUUID();
        String message = "test-message";
        RuntimeException exception = new RuntimeException("Operation failed");
        Runnable operation = mock(Runnable.class);

        when(resolvingManager.resolve(message)).thenReturn(eventId);
        when(consumedOutboxManager.isConsumed(eventId)).thenReturn(false);
        doThrow(exception).when(operation).run();
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when + then
        assertThatThrownBy(() -> consumer.consume(message, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Operation failed");
        verify(operation).run();
    }

    @Test
    @DisplayName("UT consume() when transaction fails should rethrow exception")
    void consume_whenTransactionFails_shouldRethrowException() {
        // given
        UUID eventId = UUID.randomUUID();
        String message = "test-message";
        RuntimeException exception = new RuntimeException("Transaction failed");
        Runnable operation = mock(Runnable.class);

        when(resolvingManager.resolve(message)).thenReturn(eventId);
        doThrow(exception).when(transactionTemplate).executeWithoutResult(any());

        // when + then
        assertThatThrownBy(() -> consumer.consume(message, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transaction failed");
        verify(operation, never()).run();
    }

    @Test
    @DisplayName("UT consume() batch when no events consumed should execute operation with all messages")
    void consumeBatch_whenNoEventsConsumed_shouldExecuteOperationWithAllMessages() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String msg1 = "msg1";
        String msg2 = "msg2";
        List<String> messages = List.of(msg1, msg2);
        Map<UUID, String> messageMap = new HashMap<>(Map.of(id1, msg1, id2, msg2));
        Consumer<List<String>> operation = mock(Consumer.class);

        when(resolvingManager.resolve(messages)).thenReturn(messageMap);
        when(consumedOutboxManager.filterConsumed(any())).thenReturn(Set.of());
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        consumer.consume(messages, operation);

        // then
        verify(resolvingManager).resolve(messages);
        verify(transactionTemplate).executeWithoutResult(any());
        verify(consumedOutboxManager).filterConsumed(Set.of(id1, id2));

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(operation).accept(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(msg1, msg2);
    }

    @Test
    @DisplayName("UT consume() batch when all events consumed should not execute operation")
    void consumeBatch_whenAllEventsConsumed_shouldNotExecuteOperation() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String msg1 = "msg1";
        String msg2 = "msg2";
        List<String> messages = List.of(msg1, msg2);
        Map<UUID, String> messageMap = new HashMap<>(Map.of(id1, msg1, id2, msg2));
        Consumer<List<String>> operation = mock(Consumer.class);

        when(resolvingManager.resolve(messages)).thenReturn(messageMap);
        when(consumedOutboxManager.filterConsumed(any())).thenReturn(Set.of(id1, id2));
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        consumer.consume(messages, operation);

        // then
        verify(resolvingManager).resolve(messages);
        verify(transactionTemplate).executeWithoutResult(any());
        verify(consumedOutboxManager).filterConsumed(any());
        verify(operation, never()).accept(any());
    }

    @Test
    @DisplayName("UT consume() batch when some events consumed should execute operation with unconsumed messages")
    void consumeBatch_whenSomeEventsConsumed_shouldExecuteOperationWithUnconsumedMessages() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        String msg1 = "msg1";
        String msg2 = "msg2";
        String msg3 = "msg3";
        List<String> messages = List.of(msg1, msg2, msg3);
        Map<UUID, String> messageMap = new HashMap<>(Map.of(id1, msg1, id2, msg2, id3, msg3));
        Consumer<List<String>> operation = mock(Consumer.class);

        when(resolvingManager.resolve(messages)).thenReturn(messageMap);
        when(consumedOutboxManager.filterConsumed(any())).thenReturn(Set.of(id1));
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        consumer.consume(messages, operation);

        // then
        verify(resolvingManager).resolve(messages);
        verify(transactionTemplate).executeWithoutResult(any());
        verify(consumedOutboxManager).filterConsumed(any());
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(operation).accept(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(msg2, msg3);
    }

    @Test
    @DisplayName("UT consume() batch with empty list should not execute operation")
    void consumeBatch_withEmptyList_shouldNotExecuteOperation() {
        // given
        List<String> messages = List.of();
        Map<UUID, String> messageMap = new HashMap<>();
        Consumer<List<String>> operation = mock(Consumer.class);

        when(resolvingManager.resolve(messages)).thenReturn(messageMap);
        when(consumedOutboxManager.filterConsumed(any())).thenReturn(Set.of());
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        consumer.consume(messages, operation);

        // then
        verify(resolvingManager).resolve(messages);
        verify(transactionTemplate).executeWithoutResult(any());
        verify(operation, never()).accept(any());
    }

    @Test
    @DisplayName("UT consume() batch when operation throws exception should rethrow")
    void consumeBatch_whenOperationThrowsException_shouldRethrow() {
        // given
        UUID id1 = UUID.randomUUID();
        String msg1 = "msg1";
        List<String> messages = List.of(msg1);
        Map<UUID, String> messageMap = new HashMap<>(Map.of(id1, msg1));
        RuntimeException exception = new RuntimeException("Batch operation failed");
        Consumer<List<String>> operation = mock(Consumer.class);

        when(resolvingManager.resolve(messages)).thenReturn(messageMap);
        when(consumedOutboxManager.filterConsumed(any())).thenReturn(Set.of());
        doThrow(exception).when(operation).accept(any());
        doAnswer(invocation -> {
            Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when + then
        assertThatThrownBy(() -> consumer.consume(messages, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Batch operation failed");
        verify(operation).accept(any());
    }

    @Test
    @DisplayName("UT consume() batch when transaction fails should rethrow exception")
    void consumeBatch_whenTransactionFails_shouldRethrowException() {
        // given
        UUID id1 = UUID.randomUUID();
        String msg1 = "msg1";
        List<String> messages = List.of(msg1);
        Map<UUID, String> messageMap = Map.of(id1, msg1);
        RuntimeException exception = new RuntimeException("Transaction failed");
        Consumer<List<String>> operation = mock(Consumer.class);

        when(resolvingManager.resolve(messages)).thenReturn(messageMap);
        doThrow(exception).when(transactionTemplate).executeWithoutResult(any());

        // when + then
        assertThatThrownBy(() -> consumer.consume(messages, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transaction failed");
        verify(operation, never()).accept(any());
    }
}