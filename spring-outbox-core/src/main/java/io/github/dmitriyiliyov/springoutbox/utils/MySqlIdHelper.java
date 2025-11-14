package io.github.dmitriyiliyov.springoutbox.utils;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MySqlIdHelper implements SqlIdHelper {

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

    private byte[] uuidToBytes(UUID id) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(id.getMostSignificantBits());
        byteBuffer.putLong(id.getLeastSignificantBits());
        return byteBuffer.array();
    }
}
