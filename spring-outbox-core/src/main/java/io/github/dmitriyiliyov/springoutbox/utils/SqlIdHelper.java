package io.github.dmitriyiliyov.springoutbox.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public interface SqlIdHelper {
    void setIdToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException;
    void setIdsToPs(PreparedStatement ps, int initialParameterIndex, Set<UUID> ids) throws SQLException;
}
