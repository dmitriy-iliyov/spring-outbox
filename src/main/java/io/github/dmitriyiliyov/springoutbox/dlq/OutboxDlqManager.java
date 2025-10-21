package io.github.dmitriyiliyov.springoutbox.dlq;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxDlqManager {
    void saveBatch(List<OutboxDlqEvent> events);

    long count();

    long countByStatus(DlqStatus status);

    long countByEventTypeAndStatus(String eventType, DlqStatus status);

    List<OutboxDlqEvent> loadBatch(DlqStatus status, int batchSize);

    List<OutboxDlqEvent> loadBatchByStatus(DlqStatus status, int batchNumber, int batchSize);

    void updateStatus(UUID id, DlqStatus status);

    void updateBatchStatus(Set<UUID> ids, DlqStatus status);

    void deleteBatch(Set<UUID> ids);
}
