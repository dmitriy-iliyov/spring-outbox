package io.github.dmitriyiliyov.springoutbox.tests.e2e.repository;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PostgresTestOutboxRepository implements TestOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresTestOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int countEvents() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Integer.class);
    }

    @Override
    public int countEventsByStatus(EventStatus status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE status = ?", Integer.class, status.name()
        );
    }

    @Override
    public List<UUID> findEventIdsByStatus(EventStatus status) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM outbox_events WHERE status = ?", UUID.class, status.name()
        );
    }

    @Override
    public List<UUID> findEventIds() {
        return jdbcTemplate.queryForList("SELECT id FROM outbox_events", UUID.class);
    }

    @Override
    public int findRetryCount(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT retry_count FROM outbox_events WHERE id = ?", Integer.class, id
        );
    }

    @Override
    public UUID insertEvent(String eventType, EventStatus status, String payloadType, String payload, Duration age) {
        UUID id = UUID.randomUUID();
        // Timestamps are written the same way the library does (Timestamp.from(Instant)),
        // so threshold-based workers see consistent values
        Timestamp timestamp = Timestamp.from(Instant.now().minus(age));
        jdbcTemplate.update("""
                INSERT INTO outbox_events (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)
                """,
                id, status.name(), eventType, payloadType, payload, timestamp, timestamp, timestamp
        );
        return id;
    }

    @Override
    public void shiftEventTimestamps(UUID id, Duration shift) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET next_retry_at = next_retry_at - make_interval(secs => ?),
                    created_at = created_at - make_interval(secs => ?),
                    updated_at = updated_at - make_interval(secs => ?)
                WHERE id = ?
                """,
                shift.toSeconds(), shift.toSeconds(), shift.toSeconds(), id
        );
    }

    @Override
    public int countDlqEvents() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_dlq_events", Integer.class);
    }

    @Override
    public int countDlqEventsByStatus(DlqStatus status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?", Integer.class, status.name()
        );
    }

    @Override
    public List<UUID> findDlqEventIdsByStatus(DlqStatus status) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM outbox_dlq_events WHERE dlq_status = ?", UUID.class, status.name()
        );
    }

    @Override
    public UUID insertDlqEvent(String eventType, DlqStatus dlqStatus, String payloadType, String payload) {
        UUID id = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO outbox_dlq_events (id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?)
                """,
                id, EventStatus.FAILED.name(), dlqStatus.name(), eventType, payloadType, payload,
                timestamp, timestamp, timestamp, timestamp
        );
        return id;
    }

    @Override
    public void shiftDlqEventTimestamps(UUID id, Duration shift) {
        jdbcTemplate.update("""
                UPDATE outbox_dlq_events
                SET next_retry_at = next_retry_at - make_interval(secs => ?),
                    created_at = created_at - make_interval(secs => ?),
                    updated_at = updated_at - make_interval(secs => ?),
                    moved_at = moved_at - make_interval(secs => ?)
                WHERE id = ?
                """,
                shift.toSeconds(), shift.toSeconds(), shift.toSeconds(), shift.toSeconds(), id
        );
    }

    @Override
    public boolean isConsumed(UUID eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_consumed_events WHERE id = ?", Integer.class, eventId
        ) > 0;
    }

    @Override
    public void shiftConsumedEventTimestamp(UUID eventId, Duration shift) {
        jdbcTemplate.update(
                "UPDATE outbox_consumed_events SET consumed_at = consumed_at - make_interval(secs => ?) WHERE id = ?",
                shift.toSeconds(), eventId
        );
    }

    @Override
    public void saveProducedBusiness(UUID verifyId) {
        jdbcTemplate.update("INSERT INTO e2e_produced_events (verify_id) VALUES (?)", verifyId);
    }

    @Override
    public int countProducedBusiness(UUID verifyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM e2e_produced_events WHERE verify_id = ?", Integer.class, verifyId
        );
    }

    @Override
    public void saveConsumedBusiness(UUID verifyId) {
        jdbcTemplate.update("INSERT INTO e2e_consumed_events (verify_id) VALUES (?)", verifyId);
    }

    @Override
    public int countConsumedBusiness(UUID verifyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM e2e_consumed_events WHERE verify_id = ?", Integer.class, verifyId
        );
    }

    @Override
    public int countConsumedBusiness() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e2e_consumed_events", Integer.class);
    }

    @Override
    public void truncateAll() {
        jdbcTemplate.execute("""
                TRUNCATE outbox_events, outbox_dlq_events, outbox_consumed_events, e2e_produced_events, e2e_consumed_events
                """);
    }
}
