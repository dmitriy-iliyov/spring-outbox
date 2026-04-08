package io.github.dmitriyiliyov.springoutbox.tests.e2e.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@FunctionalInterface
public interface IdExtractor {
    UUID extract(ResultSet rs) throws SQLException;
}
