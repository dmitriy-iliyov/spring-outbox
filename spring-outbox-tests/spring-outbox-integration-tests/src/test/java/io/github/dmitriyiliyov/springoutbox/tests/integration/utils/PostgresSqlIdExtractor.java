package io.github.dmitriyiliyov.springoutbox.tests.integration.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PostgresSqlIdExtractor implements IdExtractor {

    @Override
    public UUID extract(String columnName, ResultSet rs) throws SQLException {
        return rs.getObject(columnName, UUID.class);
    }
}
