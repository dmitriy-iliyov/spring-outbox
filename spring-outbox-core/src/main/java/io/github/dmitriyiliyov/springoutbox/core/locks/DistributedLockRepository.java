package io.github.dmitriyiliyov.springoutbox.core.locks;

import java.util.UUID;

/**
 * Repository interface for managing distributed locks.
 * <p>
 * Ensures that background jobs or scheduled tasks are executed by only one
 * worker instance at a time in a clustered or multi-node environment.
 */
public interface DistributedLockRepository {

    /**
     * Attempts to acquire a lock for the specified job.
     * <p>
     * If the lock is currently held by another worker and has not expired,
     * this method will return {@code false}.
     *
     * @param jobName  the unique name of the job to lock.
     * @param workerId the unique identifier of the worker attempting to acquire the lock.
     * @return         {@code true} if the lock was successfully acquired; {@code false} otherwise.
     */
    boolean tryLock(String jobName, UUID workerId);

    /**
     * Releases the lock for the specified job.
     * <p>
     * The lock will only be released if it is currently held by the specified worker.
     * This prevents workers from accidentally releasing locks they do not own.
     *
     * @param jobName  the unique name of the locked job.
     * @param workerId the unique identifier of the worker currently holding the lock.
     */
    void unlock(String jobName, UUID workerId);
}