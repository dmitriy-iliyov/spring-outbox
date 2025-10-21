package io.github.dmitriyiliyov.springoutbox.core.dlq;

import java.util.List;

public interface OutboxDlqManager {
    void saveBatch(List<OutboxDlqEvent> events);

    List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize);

    long count();

    long countByStatus(DlqStatus status);

    long countByEventTypeAndStatus(String eventType, DlqStatus status);
}
