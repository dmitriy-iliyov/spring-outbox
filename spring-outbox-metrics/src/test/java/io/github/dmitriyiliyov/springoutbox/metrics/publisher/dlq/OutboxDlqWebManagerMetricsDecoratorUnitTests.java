package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.web.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.web.BatchUpdateRequest;
import io.github.dmitriyiliyov.springoutbox.web.OutboxDlqWebManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxDlqWebManagerMetricsDecoratorUnitTests {

    @Mock
    private OutboxDlqWebManager delegate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter mockCounter;

    private OutboxDlqWebManagerMetricsDecorator tested;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(mockCounter);
        tested = new OutboxDlqWebManagerMetricsDecorator(meterRegistry, delegate);
    }

    @Test
    @DisplayName("UT findById() should strictly delegate call")
    void findById_shouldDelegate() {
        UUID id = UUID.randomUUID();
        OutboxDlqEvent expectedEvent = mock(OutboxDlqEvent.class);
        when(delegate.findById(id)).thenReturn(expectedEvent);

        OutboxDlqEvent actualEvent = tested.findById(id);

        assertEquals(expectedEvent, actualEvent);
        verify(delegate).findById(id);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT findBatch() should strictly delegate call")
    void findBatch_shouldDelegate() {
        BatchRequest request = mock(BatchRequest.class);
        List<OutboxDlqEvent> expectedList = List.of(mock(OutboxDlqEvent.class));
        when(delegate.findBatch(request)).thenReturn(expectedList);

        List<OutboxDlqEvent> actualList = tested.findBatch(request);

        assertEquals(expectedList, actualList);
        verify(delegate).findBatch(request);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT count() should strictly delegate call")
    void count_shouldDelegate() {
        DlqStatus status = DlqStatus.MOVED;
        when(delegate.count(status)).thenReturn(42L);

        long actualCount = tested.count(status);

        assertEquals(42L, actualCount);
        verify(delegate).count(status);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT updateStatus() should strictly delegate call")
    void updateStatus_shouldDelegate() {
        UUID id = UUID.randomUUID();
        DlqStatus status = DlqStatus.RESOLVED;

        tested.updateStatus(id, status);

        verify(delegate).updateStatus(id, status);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT updateBatchStatus() should strictly delegate call")
    void updateBatchStatus_shouldDelegate() {
        BatchUpdateRequest request = mock(BatchUpdateRequest.class);

        tested.updateBatchStatus(request);

        verify(delegate).updateBatchStatus(request);
        verifyNoInteractions(mockCounter);
    }

    @Test
    @DisplayName("UT deleteById() should delegate and increment manual deleted counter")
    void deleteById_shouldDelegateAndIncrementCounter() {
        UUID id = UUID.randomUUID();
        int expectedDeletedCount = 1;
        when(delegate.deleteById(id)).thenReturn(expectedDeletedCount);

        int actualDeletedCount = tested.deleteById(id);

        assertEquals(expectedDeletedCount, actualDeletedCount);
        verify(delegate).deleteById(id);
        verify(mockCounter).increment((double) expectedDeletedCount);
    }

    @Test
    @DisplayName("UT deleteBatch() should delegate and increment manual deleted counter by deleted amount")
    void deleteBatch_shouldDelegateAndIncrementCounter() {
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        int expectedDeletedCount = 2;
        when(delegate.deleteBatch(ids)).thenReturn(expectedDeletedCount);

        int actualDeletedCount = tested.deleteBatch(ids);

        assertEquals(expectedDeletedCount, actualDeletedCount);
        verify(delegate).deleteBatch(ids);
        verify(mockCounter).increment((double) expectedDeletedCount);
    }

    @Test
    @DisplayName("UT deleteBatch() when nothing deleted should increment counter by 0")
    void deleteBatch_whenNothingDeleted_shouldIncrementCounterByZero() {
        Set<UUID> ids = Set.of(UUID.randomUUID());
        int expectedDeletedCount = 0;
        when(delegate.deleteBatch(ids)).thenReturn(expectedDeletedCount);

        int actualDeletedCount = tested.deleteBatch(ids);

        assertEquals(expectedDeletedCount, actualDeletedCount);
        verify(delegate).deleteBatch(ids);
        verify(mockCounter).increment(0.0);
    }
}
