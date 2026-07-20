package io.github.dmitriyiliyov.oncebox.tests.integration.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@FunctionalInterface
public interface IdExtractor {
    UUID extract(String columnName, ResultSet rs) throws SQLException;
}
