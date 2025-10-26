package io.github.dmitriyiliyov.springoutbox.utils;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.dlq.OutboxDlqEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public final class ResultSetMapper {

    public static OutboxEvent toEvent(ResultSet rs) throws SQLException {
        return new OutboxEvent(
                rs.getObject("id", UUID.class),
                EventStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    public static OutboxDlqEvent toDlqEvent(ResultSet rs) throws SQLException {
        return new OutboxDlqEvent(
                rs.getObject("id", UUID.class),
                EventStatus.fromString(rs.getString("status")),
                rs.getString("event_type"),
                rs.getString("payload_type"),
                rs.getString("payload"),
                rs.getInt("retry_count"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                DlqStatus.fromString(rs.getString("dlq_status"))
        );
    }
}
