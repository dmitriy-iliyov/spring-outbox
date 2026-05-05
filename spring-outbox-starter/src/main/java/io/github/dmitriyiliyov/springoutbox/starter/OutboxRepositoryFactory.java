package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqRepository;

/**
 * A factory for creating repositories required by the outbox pattern.
 * <p>
 * Implementations provide specific instances of repositories tailored to the database type.
 */
public interface OutboxRepositoryFactory {
    
    /**
     * Creates an instance of {@link OutboxRepository} for managing outbox events.
     *
     * @return the created {@link OutboxRepository}.
     */
    OutboxRepository createOutboxRepository();

    /**
     * Creates an instance of {@link OutboxDlqRepository} for managing dead-letter queue outbox events.
     *
     * @return the created {@link OutboxDlqRepository}.
     */
    OutboxDlqRepository createOutboxDlqRepository();

    /**
     * Creates an instance of {@link DistributedLockRepository} for managing distributed locks.
     *
     * @return the created {@link DistributedLockRepository}.
     */
    DistributedLockRepository createDistributedLockRepository();

    /**
     * Creates an instance of {@link ConsumedOutboxRepository} for managing consumed outbox events.
     *
     * @return the created {@link ConsumedOutboxRepository}.
     */
    ConsumedOutboxRepository createConsumedOutboxRepository();
}
