package io.github.dmitriyiliyov.springoutbox.utils;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetMapper {
    OutboxEvent toEvent(ResultSet rs) throws SQLException;
    OutboxDlqEvent toDlqEvent(ResultSet rs) throws SQLException;
}
