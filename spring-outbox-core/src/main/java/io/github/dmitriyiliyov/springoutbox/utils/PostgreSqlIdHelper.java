package io.github.dmitriyiliyov.springoutbox.utils;

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
    public Set<?> convertIdsToDbFormat(Set<UUID> ids) {
        return Set.of(ids);
    }
}
