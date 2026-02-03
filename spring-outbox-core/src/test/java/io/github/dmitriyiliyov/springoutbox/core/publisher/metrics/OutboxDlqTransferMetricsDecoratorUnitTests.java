package io.github.dmitriyiliyov.springoutbox.core.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqTransfer;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxDlqTransferMetricsDecoratorUnitTests {

    @Mock
    OutboxDlqTransfer delegate;

    SimpleMeterRegistry registry;
    OutboxDlqTransferMetricsDecorator tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        tested = new OutboxDlqTransferMetricsDecorator(delegate, registry);
    }

    @Test
    @DisplayName("UT transferToDlq() should delegate and record duration")
    void transferToDlq_shouldDelegateAndRecordDuration() {
        // given
        int batchSize = 10;

        // when
        tested.transferToDlq(batchSize);

        // then
        verify(delegate).transferToDlq(batchSize);
        Timer timer = registry.get("outbox_dlq_transfer_to_duration").timer();
        assertEquals(1, timer.count());
    }

    @Test
    @DisplayName("UT transferFromDlq() should delegate and record duration")
    void transferFromDlq_shouldDelegateAndRecordDuration() {
        // given
        int batchSize = 20;

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(delegate).transferFromDlq(batchSize);
        Timer timer = registry.get("outbox_dlq_transfer_from_duration").timer();
        assertEquals(1, timer.count());
    }
}
