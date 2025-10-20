package io.github.dmitriyiliyov.springoutbox.core.dlq;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxDlqRepository {
    void saveBatch(List<OutboxDlqEvent> dlqEvents);

    List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchSize);

    void updateBatchStatus(Set<UUID> ids, DlqStatus status);
}
