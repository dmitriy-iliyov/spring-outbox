package io.github.dmitriyiliyov.springoutbox.core.locks;

import io.github.dmitriyiliyov.springoutbox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public class PostgreSqlDistributedLockRepository implements DistributedLockRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SqlIdHelper idHelper;

    public PostgreSqlDistributedLockRepository(JdbcTemplate jdbcTemplate, SqlIdHelper idHelper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
    }

    @Override
    public boolean tryLock(String jobName, UUID workerId) {
        String sql = """
            UPDATE outbox_jobs
            SET lock_until = clock_timestamp() + (lock_at_most_for * INTERVAL '1 millisecond'), locked_by = ?
            WHERE job_name = ? 
            AND lock_until <= clock_timestamp()
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    idHelper.setIdToPs(ps, 1, workerId);
                    ps.setString(2, jobName);
                }
        ) == 1;
    }

    @Override
    public void unlock(String jobName, UUID workerId) {
        String sql = """
            UPDATE outbox_jobs 
            SET lock_until = clock_timestamp() + (lock_at_least_for * INTERVAL '1 millisecond')
            WHERE job_name = ? AND locked_by = ?
        """;
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, jobName);
                    idHelper.setIdToPs(ps, 2, workerId);
                }
        );
    }
}
