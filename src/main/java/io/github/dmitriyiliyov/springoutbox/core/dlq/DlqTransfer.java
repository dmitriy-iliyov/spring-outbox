package io.github.dmitriyiliyov.springoutbox.core.dlq;

public interface DlqTransfer {
    void transferBatch(int batchSize);
}
