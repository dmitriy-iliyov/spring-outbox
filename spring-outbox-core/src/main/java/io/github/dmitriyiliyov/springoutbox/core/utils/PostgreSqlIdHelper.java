package io.github.dmitriyiliyov.springoutbox.core.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public final class PostgreSqlIdHelper implements SqlIdHelper {

    @Override
    public void setIdToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException {
        ps.setObject(parameterIndex, id);
    }

    @Override
    public void setIdsToPs(PreparedStatement ps, int initialParameterIndex, Set<UUID> ids) throws SQLException {
        for (UUID id : ids) {
            ps.setObject(initialParameterIndex++, id);
        }
    }
}
