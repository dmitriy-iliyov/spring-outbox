package io.github.dmitriyiliyov.springoutbox.dlq;

public interface OutboxDlqTransfer {
    void transferOutboxToDlq(int batchSize);
    void transferDlqToOutbox(int batchSize);
}
