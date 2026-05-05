package io.github.dmitriyiliyov.springoutbox.core.locks;

import org.springframework.jdbc.core.JdbcTemplate;

public interface SetupJobScript {
    void setup(JdbcTemplate jdbcTemplate, DistributedLockRepositoryVerifier.IdPreparer idPreparer, String jobName, Long lockAtLeastFor, Long lockAtMostFor);
}
