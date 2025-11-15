package io.github.dmitriyiliyov.springoutbox.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class BytesSqlIdHelper implements SqlIdHelper {

    public abstract byte[] uuidToBytes(UUID id);

    @Override
    public void setIdToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException {
        ps.setBytes(parameterIndex, uuidToBytes(id));
    }

    @Override
    public Set<?> convertIdsToDbFormat(Set<UUID> ids) {
        return ids.stream()
                .map(this::uuidToBytes)
                .collect(Collectors.toSet());
    }
}
