package io.github.dmitriyiliyov.springoutbox.tests.integration.utils;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class OracleIdExtractor implements IdExtractor {

    @Override
    public UUID extract(String columnName, ResultSet rs) throws SQLException {
        ByteBuffer bb = ByteBuffer.wrap(rs.getBytes(columnName));
        return new UUID(bb.getLong(), bb.getLong());
    }
}
