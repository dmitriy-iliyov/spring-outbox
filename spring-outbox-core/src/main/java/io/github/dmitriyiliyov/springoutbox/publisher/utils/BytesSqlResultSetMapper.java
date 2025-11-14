package io.github.dmitriyiliyov.springoutbox.publisher.utils;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class MySqlResultSetMapper implements ResultSetMapper {

    @Override
    public OutboxEvent toEvent(ResultSet rs) throws SQLException {
        return new OutboxEvent(
                fromBytesToUuid(rs.getBytes("id")),
                EventStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("next_retry_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    @Override
    public OutboxDlqEvent toDlqEvent(ResultSet rs) throws SQLException {
        return new OutboxDlqEvent(
                fromBytesToUuid(rs.getBytes("id")),
                EventStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("next_retry_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                DlqStatus.fromString(rs.getString("dlq_status")),
                rs.getTimestamp("moved_at").toInstant()
        );
    }

    private UUID fromBytesToUuid(byte [] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("UUID byte array must be 16 bytes long");
        }
        long mostSigBits = 0;
        long leastSigBits = 0;
        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (bytes[i] & 0xFF);
        }
        return new UUID(mostSigBits, leastSigBits);
    }
}
