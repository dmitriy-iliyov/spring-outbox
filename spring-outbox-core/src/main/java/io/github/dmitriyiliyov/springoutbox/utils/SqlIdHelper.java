package io.github.dmitriyiliyov.springoutbox.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public interface SqlIdHelper {
    void setIdToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException;
    Set<?> convertIdsToDbFormat(Set<UUID> ids);
}
