package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchUpdateRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxDlqManager {
    void saveBatch(List<OutboxDlqEvent> events);

    long count();

    long countByStatus(DlqStatus status);

    long countByEventTypeAndStatus(String eventType, DlqStatus status);

    OutboxDlqEvent loadById(UUID id);

    List<OutboxDlqEvent> loadBatch(DlqStatus status, int batchSize);

    List<OutboxDlqEvent> loadBatch(BatchRequest request);

    void updateStatus(UUID id, DlqStatus status);

    void updateBatchStatus(BatchUpdateRequest request);

    void deleteBatch(Set<UUID> ids);

    void deleteById(UUID id);
}
