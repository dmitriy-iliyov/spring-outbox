package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultOutboxDlqTransferUnitTests {

    @Mock
    TransactionTemplate transactionTemplate;

    @Mock
    OutboxManager manager;

    @Mock
    OutboxDlqManager dlqManager;

    @Mock
    OutboxDlqHandler handler;

    @InjectMocks
    DefaultOutboxDlqTransfer tested;

    @BeforeEach
    void setupTx() {
        lenient().doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("UT transferToDlq() when events is empty, should not transfer and not call handler")
    void transferToDlq_whenEventsIsEmpty_shouldNotTransferAndNotCallHandler() {
        // given
        int batchSize = 50;
        when(manager.loadBatch(EventStatus.FAILED, batchSize)).thenReturn(List.of());

        // when
        tested.transferToDlq(batchSize);

        // then
        verify(manager).loadBatch(EventStatus.FAILED, batchSize);
        verify(dlqManager, never()).saveBatch(anyList());
        verify(manager, never()).deleteBatch(anySet());
        verify(handler, never()).handle(anyList());
    }

    @Test
    @DisplayName("UT transferToDlq() when events exist, should transfer and call handler")
    void transferToDlq_whenEventsExist_shouldTransferAndCallHandler() {
        // given
        int batchSize = 50;
        UUID eventId = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                eventId,
                EventStatus.FAILED,
                "type",
                "type",
                "{}",
                1,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        when(manager.loadBatch(EventStatus.FAILED, batchSize)).thenReturn(List.of(event));

        // when
        tested.transferToDlq(batchSize);

        // then
        verify(manager).loadBatch(EventStatus.FAILED, batchSize);
        verify(dlqManager).saveBatch(anyList());
        verify(manager).deleteBatch(Set.of(eventId));
        verify(handler).handle(List.of(event));
    }

    @Test
    @DisplayName("UT transferToDlq() when exception in transaction, should not call handler")
    void transferToDlq_whenExceptionInTransaction_shouldNotCallHandler() {
        // given
        int batchSize = 50;
        UUID eventId = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                eventId,
                EventStatus.FAILED,
                "OrderCreated",
                "com.example.OrderCreatedEvent",
                "{\"orderId\":\"123\"}",
                3,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        List<OutboxEvent> events = List.of(event);

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(manager.loadBatch(EventStatus.FAILED, batchSize)).thenReturn(events);
            doThrow(new RuntimeException("Database error")).when(dlqManager).saveBatch(anyList());
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        assertThrows(RuntimeException.class, () -> tested.transferToDlq(batchSize));

        // then
        verify(manager).loadBatch(EventStatus.FAILED, batchSize);
        verify(dlqManager).saveBatch(anyList());
        verify(manager, never()).deleteBatch(anySet());
        verify(handler, never()).handle(anyList());
    }

    @Test
    @DisplayName("UT transferToDlq() when handler throws exception, should not affect transaction")
    void transferToDlq_whenHandlerThrows_shouldNotAffectTransaction() {
        // given
        int batchSize = 50;

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), EventStatus.FAILED, "t", "t", "{}", 1,
                Instant.now(), Instant.now(), Instant.now()
        );

        when(manager.loadBatch(any(EventStatus.class), anyInt())).thenReturn(List.of(event));
        doThrow(new RuntimeException()).when(handler).handle(anyList());

        // when / then
        assertDoesNotThrow(() -> tested.transferToDlq(batchSize));

        verify(manager).loadBatch(any(EventStatus.class), anyInt());
        verify(dlqManager).saveBatch(anyList());
        verify(manager).deleteBatch(anySet());
        verify(handler).handle(List.of(event));
    }

    @Test
    @DisplayName("UT transferFromDlq() when dlq events is null, should not transfer")
    void transferFromDlq_whenDlqEventsIsNull_shouldNotTransferFrom() {
        // given
        int batchSize = 50;

        when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(null);

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(dlqManager).loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
        verify(manager, never()).saveBatch(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferFromDlq() when dlq events is empty, should not transfer")
    void transferFromDlq_whenDlqEventsIsEmpty_shouldNotTransferFrom() {
        // given
        int batchSize = 50;

        when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(List.of());

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(dlqManager).loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
        verify(manager, never()).saveBatch(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferFromDlq() when dlq events exist, should transfer to outbox")
    void transferFromDlq_whenDlqEventsExist_shouldTransferFrom() {
        // given
        int batchSize = 50;
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(1000);

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                eventId,
                EventStatus.FAILED,
                "type",
                "type",
                "{}",
                5,
                Instant.now(),
                createdAt,
                Instant.now(),
                DlqStatus.TO_RETRY,
                Instant.now()
        );

        when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(List.of(dlqEvent));

        // when
        tested.transferFromDlq(batchSize);

        // then
        ArgumentCaptor<List<OutboxEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(manager).saveBatch(captor.capture());

        OutboxEvent saved = captor.getValue().get(0);

        assertEquals(eventId, saved.getId());
        assertEquals(EventStatus.PENDING, saved.getStatus());
        assertEquals(-1, saved.getRetryCount());
        assertEquals(createdAt, saved.getCreatedAt());

        verify(dlqManager).deleteBatch(Set.of(eventId));
    }

    @Test
    @DisplayName("UT transferFromDlq() when exception occurs, should not delete from dlq")
    void transferFromDlq_whenExceptionOccurs_shouldNotDeleteFromFromDlq() {
        // given
        int batchSize = 50;

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                UUID.randomUUID(),
                EventStatus.FAILED,
                "type",
                "type",
                "{}",
                1,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                DlqStatus.TO_RETRY,
                Instant.now()
        );

        when(dlqManager.loadAndLockBatch(any(), anyInt())).thenReturn(List.of(dlqEvent));
        doThrow(new RuntimeException()).when(manager).saveBatch(anyList());

        // when / then
        assertThrows(RuntimeException.class, () -> tested.transferFromDlq(batchSize));

        verify(dlqManager).loadAndLockBatch(any(), anyInt());
        verify(manager).saveBatch(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferFromDlq() should reset retry count to -1")
    void transferFromDlq_shouldResetRetryCountToMinusOne() {
        // given
        int batchSize = 50;

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                UUID.randomUUID(),
                EventStatus.FAILED,
                "type",
                "type",
                "{}",
                5,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                DlqStatus.TO_RETRY,
                Instant.now()
        );

        when(dlqManager.loadAndLockBatch(any(), anyInt())).thenReturn(List.of(dlqEvent));

        // when
        tested.transferFromDlq(batchSize);

        // then
        ArgumentCaptor<List<OutboxEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(manager).saveBatch(captor.capture());

        assertEquals(-1, captor.getValue().get(0).getRetryCount());
    }

    @Test
    @DisplayName("UT transferFromDlq() should preserve original created_at timestamp")
    void transferFromDlq_shouldPreserveOriginalCreatedAt() {
        // given
        int batchSize = 50;
        Instant createdAt = Instant.now().minusSeconds(86400);

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                UUID.randomUUID(),
                EventStatus.FAILED,
                "type",
                "type",
                "{}",
                3,
                Instant.now(),
                createdAt,
                Instant.now(),
                DlqStatus.TO_RETRY,
                Instant.now()
        );

        when(dlqManager.loadAndLockBatch(any(), anyInt())).thenReturn(List.of(dlqEvent));

        // when
        tested.transferFromDlq(batchSize);

        // then
        ArgumentCaptor<List<OutboxEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(manager).saveBatch(captor.capture());

        assertEquals(createdAt, captor.getValue().get(0).getCreatedAt());
    }
}