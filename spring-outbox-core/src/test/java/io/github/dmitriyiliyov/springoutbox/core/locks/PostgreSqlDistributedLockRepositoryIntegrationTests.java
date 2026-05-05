package io.github.dmitriyiliyov.springoutbox.core.locks;

import io.github.dmitriyiliyov.springoutbox.core.it.BasePostgresSqlIntegrationTests;
import io.github.dmitriyiliyov.springoutbox.core.utils.PostgreSqlIdHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Transactional
class PostgreSqlDistributedLockRepositoryIntegrationTests extends BasePostgresSqlIntegrationTests {

    private DistributedLockRepositoryVerifier verifier;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgreSqlDistributedLockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PostgreSqlDistributedLockRepository(jdbcTemplate, new PostgreSqlIdHelper());
        this.verifier = new DistributedLockRepositoryVerifier(
                jdbcTemplate,
                repository,
                raw -> (UUID) raw,
                id -> id,
                (jdbcTemplate, idPreparer, jobName, lockAtLeastFor, lockAtMostFor) -> {
                    String sql = "INSERT INTO outbox_jobs (job_name, lock_until, locked_by, lock_at_least_for, lock_at_most_for) VALUES (?, ?, ?, ?, ?)";
                    jdbcTemplate.update(sql, jobName, Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)), idPreparer.prepare(UUID.randomUUID()), lockAtLeastFor, lockAtMostFor);
                }
        );
    }

    @Test
    @DisplayName("IT tryLock() should lock job when available")
    void tryLock_jobAvailable_locksSuccessfully() {
        verifier.tryLock_jobAvailable_locksSuccessfully();
    }

    @Test
    @DisplayName("IT tryLock() should return false when job already locked")
    void tryLock_jobAlreadyLocked_returnsFalse() {
        verifier.tryLock_jobAlreadyLocked_returnsFalse();
    }

    @Test
    @DisplayName("IT tryLock() should lock job when lock expired")
    void tryLock_lockExpired_locksSuccessfully() {
        verifier.tryLock_lockExpired_locksSuccessfully();
    }

    @Test
    @DisplayName("IT unlock() should unlock job for valid worker")
    void unlock_validWorker_unlocksJob() {
        verifier.unlock_validWorker_unlocksJob();
    }

    @Test
    @DisplayName("IT unlock() should not unlock for different worker")
    void unlock_differentWorker_doesNotUnlock() {
        verifier.unlock_differentWorker_doesNotUnlock();
    }

    @Test
    @DisplayName("IT tryLock() should return false for non-existent job")
    void tryLock_nonExistentJob_returnsFalse() {
        verifier.tryLock_nonExistentJob_returnsFalse();
    }
}