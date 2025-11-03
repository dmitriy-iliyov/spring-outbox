package io.github.dmitriyiliyov.springoutbox.dlq;

public interface OutboxDlqTransfer {
    void transferToDlq(int batchSize);
    void transferFromDlq(int batchSize);
}
