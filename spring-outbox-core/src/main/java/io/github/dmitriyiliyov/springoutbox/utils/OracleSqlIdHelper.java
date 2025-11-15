package io.github.dmitriyiliyov.springoutbox.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class OracleSqlIdHelper extends BytesSqlIdHelper {

    @Override
    public byte[] uuidToBytes(UUID id) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(id.getMostSignificantBits());
        byteBuffer.putLong(id.getLeastSignificantBits());
        return byteBuffer.array();
    }
}
