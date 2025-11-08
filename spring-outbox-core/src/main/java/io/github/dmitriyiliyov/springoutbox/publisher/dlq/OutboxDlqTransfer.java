package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

public interface OutboxDlqTransfer {
    void transferToDlq(int batchSize);
    void transferFromDlq(int batchSize);
}
