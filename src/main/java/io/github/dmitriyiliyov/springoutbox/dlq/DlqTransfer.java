package io.github.dmitriyiliyov.springoutbox.dlq;

public interface DlqTransfer {
    void transferOutboxToDlq(int batchSize);
    void transferDlqToOutbox(int batchSize);
}
