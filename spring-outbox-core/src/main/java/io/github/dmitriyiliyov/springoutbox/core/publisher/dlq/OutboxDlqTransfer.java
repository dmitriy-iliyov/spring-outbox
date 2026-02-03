package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

public interface OutboxDlqTransfer {
    void transferToDlq(int batchSize);
    void transferFromDlq(int batchSize);
}
