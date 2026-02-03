package io.github.dmitriyiliyov.springoutbox.core.utils;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetMapper {
    OutboxEvent toEvent(ResultSet rs) throws SQLException;
    OutboxDlqEvent toDlqEvent(ResultSet rs) throws SQLException;
}
