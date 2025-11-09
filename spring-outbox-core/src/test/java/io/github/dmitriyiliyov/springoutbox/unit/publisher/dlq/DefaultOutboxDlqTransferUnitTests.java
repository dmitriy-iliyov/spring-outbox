package io.github.dmitriyiliyov.springoutbox.unit.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import org.junit.jupiter.api.Assertions;
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

    @Test
    @DisplayName("UT transferOutboxToDlq() when events is empty, should not transfer and not call handler")
    public void transferOutboxToDlq_whenEventsIsEmpty_shouldNotTransferAndNotCallHandler() {
        // given
        int batchSize = 50;

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(manager.loadBatch(EventStatus.FAILED, batchSize)).thenReturn(List.of());
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        tested.transferToDlq(batchSize);

        // then
        verify(manager).loadBatch(EventStatus.FAILED, batchSize);
        verify(dlqManager, never()).saveBatch(anyList());
        verify(manager, never()).deleteBatch(anySet());
        verify(handler, never()).handle(anyList());
    }

    @Test
    @DisplayName("UT transferOutboxToDlq() when events exist, should transfer to dlq and call handler")
    public void transferOutboxToDlq_whenEventsExist_shouldTransferToDlqAndCallHandler() {
        // given
        int batchSize = 50;
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();

        OutboxEvent event1 = new OutboxEvent(
                eventId1,
                EventStatus.FAILED,
                "OrderCreated",
                "com.example.OrderCreatedEvent",
                "{\"orderId\":\"123\"}",
                3,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        OutboxEvent event2 = new OutboxEvent(
                eventId2,
                EventStatus.FAILED,
                "UserRegistered",
                "com.example.UserRegisteredEvent",
                "{\"userId\":\"456\"}",
                2,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        List<OutboxEvent> events = List.of(event1, event2);

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(manager.loadBatch(EventStatus.FAILED, batchSize)).thenReturn(events);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        tested.transferToDlq(batchSize);

        // then
        verify(manager).loadBatch(EventStatus.FAILED, batchSize);

        ArgumentCaptor<List<OutboxDlqEvent>> dlqEventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(dlqManager).saveBatch(dlqEventsCaptor.capture());

        List<OutboxDlqEvent> savedDlqEvents = dlqEventsCaptor.getValue();
        assertEquals(2, savedDlqEvents.size());
        assertEquals(eventId1, savedDlqEvents.get(0).getId());
        assertEquals(eventId2, savedDlqEvents.get(1).getId());
        Assertions.assertEquals(DlqStatus.NEW, savedDlqEvents.get(0).getDlqStatus());
        assertEquals(DlqStatus.NEW, savedDlqEvents.get(1).getDlqStatus());

        ArgumentCaptor<Set<UUID>> deleteIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(manager).deleteBatch(deleteIdsCaptor.capture());

        Set<UUID> deletedIds = deleteIdsCaptor.getValue();
        assertEquals(2, deletedIds.size());
        assertTrue(deletedIds.contains(eventId1));
        assertTrue(deletedIds.contains(eventId2));

        verify(handler).handle(events);
    }

    @Test
    @DisplayName("UT transferOutboxToDlq() when exception in transaction, should not call handler")
    public void transferToDlq_whenExceptionInTransaction_shouldNotCallHandler() {
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
    @DisplayName("UT transferOutboxToDlq() when handler throws exception, should not affect transaction")
    public void transferToDlq_whenHandlerThrows_shouldNotAffectTransaction() {
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
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        doThrow(new RuntimeException("Handler error")).when(handler).handle(anyList());

        // when/then - should not throw
        assertDoesNotThrow(() -> tested.transferToDlq(batchSize));

        verify(manager).loadBatch(EventStatus.FAILED, batchSize);
        verify(dlqManager).saveBatch(anyList());
        verify(manager).deleteBatch(anySet());
        verify(handler).handle(events);
    }

    @Test
    @DisplayName("UT transferDlqToOutbox() when dlq events is null, should not transfer")
    public void transferDlqToOutbox_whenDlqEventsIsNull_shouldNotTransferFrom() {
        // given
        int batchSize = 50;

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(null);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(dlqManager).loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
        verify(manager, never()).saveBatch(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferDlqToOutbox() when dlq events is empty, should not transfer")
    public void transferDlqToOutbox_whenDlqEventsIsEmpty_shouldNotTransferFrom() {
        // given
        int batchSize = 50;

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(List.of());
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(dlqManager).loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
        verify(manager, never()).saveBatch(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferDlqToOutbox() when dlq events exist, should transfer to outbox")
    public void transferDlqToOutbox_whenDlqEventsExist_shouldTransferFrom() {
        // given
        int batchSize = 50;
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxDlqEvent dlqEvent1 = new OutboxDlqEvent(
                eventId1,
                EventStatus.FAILED,
                "OrderCreated",
                "com.example.OrderCreatedEvent",
                "{\"orderId\":\"123\"}",
                3,
                Instant.now(),
                now.minusSeconds(3600),
                now,
                DlqStatus.TO_RETRY
        );

        OutboxDlqEvent dlqEvent2 = new OutboxDlqEvent(
                eventId2,
                EventStatus.FAILED,
                "UserRegistered",
                "com.example.UserRegisteredEvent",
                "{\"userId\":\"456\"}",
                2,
                Instant.now(),
                now.minusSeconds(7200),
                now,
                DlqStatus.TO_RETRY
        );

        List<OutboxDlqEvent> dlqEvents = List.of(dlqEvent1, dlqEvent2);

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(dlqEvents);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(dlqManager).loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);

        ArgumentCaptor<List<OutboxEvent>> outboxEventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(manager).saveBatch(outboxEventsCaptor.capture());

        List<OutboxEvent> savedOutboxEvents = outboxEventsCaptor.getValue();
        assertEquals(2, savedOutboxEvents.size());
        assertEquals(eventId1, savedOutboxEvents.get(0).getId());
        assertEquals(eventId2, savedOutboxEvents.get(1).getId());
        assertEquals(EventStatus.PENDING, savedOutboxEvents.get(0).getStatus());
        assertEquals(EventStatus.PENDING, savedOutboxEvents.get(1).getStatus());
        assertEquals(0, savedOutboxEvents.get(0).getRetryCount());
        assertEquals(0, savedOutboxEvents.get(1).getRetryCount());

        ArgumentCaptor<Set<UUID>> deleteIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(dlqManager).deleteBatch(deleteIdsCaptor.capture());

        Set<UUID> deletedIds = deleteIdsCaptor.getValue();
        assertEquals(2, deletedIds.size());
        assertTrue(deletedIds.contains(eventId1));
        assertTrue(deletedIds.contains(eventId2));
    }

    @Test
    @DisplayName("UT transferDlqToOutbox() when exception occurs, should not delete from dlq")
    public void transferDlqToOutbox_whenExceptionOccurs_shouldNotDeleteFromFromDlq() {
        // given
        int batchSize = 50;
        UUID eventId = UUID.randomUUID();

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                eventId,
                EventStatus.FAILED,
                "OrderCreated",
                "com.example.OrderCreatedEvent",
                "{\"orderId\":\"123\"}",
                3,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                DlqStatus.TO_RETRY
        );

        List<OutboxDlqEvent> dlqEvents = List.of(dlqEvent);

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(dlqEvents);
            doThrow(new RuntimeException("Database error")).when(manager).saveBatch(anyList());
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        assertThrows(RuntimeException.class, () -> tested.transferFromDlq(batchSize));

        // then
        verify(dlqManager).loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
        verify(manager).saveBatch(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferDlqToOutbox() should reset retry count to zero")
    public void transferFromDlqToOutbox_shouldResetRetryCountToZero() {
        // given
        int batchSize = 50;
        UUID eventId = UUID.randomUUID();

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                eventId,
                EventStatus.FAILED,
                "OrderCreated",
                "com.example.OrderCreatedEvent",
                "{\"orderId\":\"123\"}",
                5,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                DlqStatus.TO_RETRY
        );

        List<OutboxDlqEvent> dlqEvents = List.of(dlqEvent);

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(dlqEvents);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        tested.transferFromDlq(batchSize);

        // then
        ArgumentCaptor<List<OutboxEvent>> outboxEventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(manager).saveBatch(outboxEventsCaptor.capture());

        List<OutboxEvent> savedOutboxEvents = outboxEventsCaptor.getValue();
        assertEquals(1, savedOutboxEvents.size());
        assertEquals(0, savedOutboxEvents.get(0).getRetryCount());
    }

    @Test
    @DisplayName("UT transferDlqToOutbox() should preserve original created_at timestamp")
    public void transferFromDlq_shouldPreserveOriginalCreatedAt() {
        // given
        int batchSize = 50;
        UUID eventId = UUID.randomUUID();
        Instant originalCreatedAt = Instant.now().minusSeconds(86400);

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                eventId,
                EventStatus.FAILED,
                "OrderCreated",
                "com.example.OrderCreatedEvent",
                "{\"orderId\":\"123\"}",
                3,
                Instant.now(),
                originalCreatedAt,
                Instant.now(),
                DlqStatus.TO_RETRY
        );

        List<OutboxDlqEvent> dlqEvents = List.of(dlqEvent);

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(dlqEvents);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when
        tested.transferFromDlq(batchSize);

        // then
        ArgumentCaptor<List<OutboxEvent>> outboxEventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(manager).saveBatch(outboxEventsCaptor.capture());

        List<OutboxEvent> savedOutboxEvents = outboxEventsCaptor.getValue();
        assertEquals(1, savedOutboxEvents.size());
        assertEquals(originalCreatedAt, savedOutboxEvents.get(0).getCreatedAt());
    }
}