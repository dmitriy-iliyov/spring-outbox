package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.Continuable;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxDlqTransferSchedulerUnitTests {

    @Mock
    OutboxPublisherPropertiesHolder.DlqPropertiesHolder properties;

    @Mock
    OutboxPublisherPropertiesHolder.DlqPropertiesHolder.TransferPropertiesHolder transferToProperties;

    @Mock
    OutboxPublisherPropertiesHolder.DlqPropertiesHolder.TransferPropertiesHolder transferFromProperties;

    @Mock
    OutboxScheduleStrategy transferToStrategy;

    @Mock
    OutboxScheduleStrategy transferFromStrategy;

    @Mock
    OutboxDlqTransfer transfer;

    OutboxDlqTransferScheduler tested;

    @BeforeEach
    void setUp() {
        tested = new OutboxDlqTransferScheduler(properties, transferToStrategy, transferFromStrategy, transfer);
    }

    private Continuable captureTransferTo() {
        ArgumentCaptor<Continuable> captor = ArgumentCaptor.forClass(Continuable.class);
        verify(transferToStrategy).scheduleExecution(captor.capture());
        return captor.getValue();
    }

    private Continuable captureTransferFrom() {
        ArgumentCaptor<Continuable> captor = ArgumentCaptor.forClass(Continuable.class);
        verify(transferFromStrategy).scheduleExecution(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("UT schedule() should delegate to both strategies")
    void schedule_shouldDelegateToBothStrategies() {
        // when
        tested.schedule();

        // then
        verify(transferToStrategy).scheduleExecution(any());
        verify(transferFromStrategy).scheduleExecution(any());
    }

    @Test
    @DisplayName("UT schedule() transferTo when transferred count equals batch size, continuable should return true")
    void schedule_transferTo_whenTransferredCountEqualsBatchSize_continuableShouldReturnTrue() {
        // given
        int batchSize = 50;
        when(properties.getTransferTo()).thenReturn(transferToProperties);
        when(transferToProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferToDlq(batchSize)).thenReturn(batchSize);

        // when
        tested.schedule();
        boolean result = captureTransferTo().run();

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("UT schedule() transferTo when transferred count less than batch size, continuable should return false")
    void schedule_transferTo_whenTransferredCountLessThanBatchSize_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        when(properties.getTransferTo()).thenReturn(transferToProperties);
        when(transferToProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferToDlq(batchSize)).thenReturn(batchSize - 1);

        // when
        tested.schedule();
        boolean result = captureTransferTo().run();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() transferTo when transferred count is zero, continuable should return false")
    void schedule_transferTo_whenTransferredCountIsZero_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        when(properties.getTransferTo()).thenReturn(transferToProperties);
        when(transferToProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferToDlq(batchSize)).thenReturn(0);

        // when
        tested.schedule();
        boolean result = captureTransferTo().run();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() transferTo should pass batchSize from properties to transfer")
    void schedule_transferTo_shouldPassBatchSizeToTransfer() {
        // given
        int batchSize = 100;
        when(properties.getTransferTo()).thenReturn(transferToProperties);
        when(transferToProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferToDlq(batchSize)).thenReturn(0);

        // when
        tested.schedule();
        captureTransferTo().run();

        // then
        verify(transfer).transferToDlq(batchSize);
    }

    @Test
    @DisplayName("UT schedule() transferTo when transfer throws exception, continuable should return false and not rethrow")
    void schedule_transferTo_whenTransferThrows_continuableShouldReturnFalseAndNotRethrow() {
        // given
        int batchSize = 50;
        when(properties.getTransferTo()).thenReturn(transferToProperties);
        when(transferToProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferToDlq(batchSize)).thenThrow(new RuntimeException("DB error"));

        // when
        tested.schedule();
        boolean result = assertDoesNotThrow(() -> captureTransferTo().run());

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() transferFrom when transferred count equals batch size, continuable should return true")
    void schedule_transferFrom_whenTransferredCountEqualsBatchSize_continuableShouldReturnTrue() {
        // given
        int batchSize = 50;
        when(properties.getTransferFrom()).thenReturn(transferFromProperties);
        when(transferFromProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferFromDlq(batchSize)).thenReturn(batchSize);

        // when
        tested.schedule();
        boolean result = captureTransferFrom().run();

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("UT schedule() transferFrom when transferred count less than batch size, continuable should return false")
    void schedule_transferFrom_whenTransferredCountLessThanBatchSize_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        when(properties.getTransferFrom()).thenReturn(transferFromProperties);
        when(transferFromProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferFromDlq(batchSize)).thenReturn(batchSize - 1);

        // when
        tested.schedule();
        boolean result = captureTransferFrom().run();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() transferFrom when transferred count is zero, continuable should return false")
    void schedule_transferFrom_whenTransferredCountIsZero_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        when(properties.getTransferFrom()).thenReturn(transferFromProperties);
        when(transferFromProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferFromDlq(batchSize)).thenReturn(0);

        // when
        tested.schedule();
        boolean result = captureTransferFrom().run();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() transferFrom should pass batchSize from properties to transfer")
    void schedule_transferFrom_shouldPassBatchSizeToTransfer() {
        // given
        int batchSize = 100;
        when(properties.getTransferFrom()).thenReturn(transferFromProperties);
        when(transferFromProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferFromDlq(batchSize)).thenReturn(0);

        // when
        tested.schedule();
        captureTransferFrom().run();

        // then
        verify(transfer).transferFromDlq(batchSize);
    }

    @Test
    @DisplayName("UT schedule() transferFrom when transfer throws exception, continuable should return false and not rethrow")
    void schedule_transferFrom_whenTransferThrows_continuableShouldReturnFalseAndNotRethrow() {
        // given
        int batchSize = 50;
        when(properties.getTransferFrom()).thenReturn(transferFromProperties);
        when(transferFromProperties.getBatchSize()).thenReturn(batchSize);
        when(transfer.transferFromDlq(batchSize)).thenThrow(new RuntimeException("DB error"));

        // when
        tested.schedule();
        boolean result = assertDoesNotThrow(() -> captureTransferFrom().run());

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() transferTo and transferFrom should use independent strategies")
    void schedule_transferToAndTransferFrom_shouldUseIndependentStrategies() {
        // when
        tested.schedule();

        // then
        verify(transferToStrategy, times(1)).scheduleExecution(any());
        verify(transferFromStrategy, times(1)).scheduleExecution(any());
        verifyNoMoreInteractions(transferToStrategy, transferFromStrategy);
    }
}