package io.github.dmitriyiliyov.springoutbox.dlq;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OutboxDlqRepository {
    void saveBatch(List<OutboxDlqEvent> dlqEvents);

    long count();

    long countByStatus(DlqStatus status);

    long countByEventTypeAndStatus(String eventType, DlqStatus status);

    Optional<OutboxDlqEvent> findById(UUID id);

    List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchSize);

    List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize);

    int updateStatus(UUID id, DlqStatus status);

    void updateBatchStatus(Set<UUID> ids, DlqStatus status);

    void deleteById(UUID id);

    void deleteBatch(Set<UUID> ids);
}
