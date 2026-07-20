package io.github.dmitriyiliyov.oncebox.core.utils;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetMapper {
    OutboxEvent toEvent(ResultSet rs) throws SQLException;
    OutboxDlqEvent toDlqEvent(ResultSet rs) throws SQLException;
}
