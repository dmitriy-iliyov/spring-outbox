package io.github.dmitriyiliyov.springoutbox.tests.e2e.repository;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Big-endian 16-byte UUID encoding matching how the library stores ids in BINARY(16)/RAW(16) columns.
 */
final class UuidBytes {

    private UuidBytes() {}

    static byte[] toBytes(UUID id) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(id.getMostSignificantBits());
        buffer.putLong(id.getLeastSignificantBits());
        return buffer.array();
    }

    static UUID fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
