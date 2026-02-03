package io.github.dmitriyiliyov.springoutbox.core.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public abstract class BytesSqlIdHelper implements SqlIdHelper {

    public abstract byte[] uuidToBytes(UUID id);

    @Override
    public void setIdToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException {
        ps.setBytes(parameterIndex, uuidToBytes(id));
    }

    @Override
    public void setIdsToPs(PreparedStatement ps, int initialParameterIndex, Set<UUID> ids) throws SQLException {
        for (UUID id : ids) {
            ps.setBytes(initialParameterIndex++, uuidToBytes(id));
        }
    }
}
