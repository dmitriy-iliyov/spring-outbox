package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchRequestProjection;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchUpdateRequestProjection;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxDlqManager {
    void saveBatch(List<OutboxDlqEvent> events);

    OutboxDlqEvent loadById(UUID id);

    List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize);

    List<OutboxDlqEvent> loadBatch(BatchRequestProjection request);

    void updateStatus(UUID id, DlqStatus status);

    void updateBatchStatus(BatchUpdateRequestProjection request);

    int deleteById(UUID id);

    int deleteBatch(Set<UUID> ids);

    int deleteBatchWithCheck(Set<UUID> ids);
}
