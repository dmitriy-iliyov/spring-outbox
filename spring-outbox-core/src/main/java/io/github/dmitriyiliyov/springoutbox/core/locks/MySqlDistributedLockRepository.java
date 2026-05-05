package io.github.dmitriyiliyov.springoutbox.core.locks;

import io.github.dmitriyiliyov.springoutbox.core.utils.BytesSqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public class MySqlDistributedLockRepository implements DistributedLockRepository {

    private final JdbcTemplate jdbcTemplate;
    private final BytesSqlIdHelper idHelper;

    public MySqlDistributedLockRepository(JdbcTemplate jdbcTemplate, BytesSqlIdHelper idHelper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
    }

    @Override
    public boolean tryLock(String jobName, UUID workerId) {
        String sql = """
            UPDATE outbox_jobs
            SET lock_until = TIMESTAMPADD(MICROSECOND, lock_at_most_for * 1000, UTC_TIMESTAMP(3)), locked_by = ?
            WHERE job_name = ? 
            AND lock_until <= UTC_TIMESTAMP(3)
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
            SET lock_until = TIMESTAMPADD(MICROSECOND, lock_at_least_for * 1000, UTC_TIMESTAMP(3))
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
