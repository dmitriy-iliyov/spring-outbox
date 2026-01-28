package io.github.dmitriyiliyov.springoutbox.unit.publisher.utils;

import io.github.dmitriyiliyov.springoutbox.utils.DefaultBytesSqlResultSetMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultBytesSqlResultSetMapperUnitTests {

    DefaultBytesSqlResultSetMapper tested = new DefaultBytesSqlResultSetMapper();

    @Test
    @DisplayName("UT fromBytesToUuid() when bytes valid should return UUID")
    void fromBytesToUuid_whenBytesValid_shouldReturnUuid() {
        // given
        UUID expected = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(expected.getMostSignificantBits());
        bb.putLong(expected.getLeastSignificantBits());
        byte[] bytes = bb.array();

        // when
        UUID result = tested.fromBytesToUuid(bytes);

        // then
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("UT fromBytesToUuid() when bytes length less than 16 should throw IAE")
    void fromBytesToUuid_whenBytesLengthLessThan16_shouldThrow() {
        // given
        byte[] bytes = new byte[15];

        // when + then
        assertThrows(IllegalArgumentException.class, () -> tested.fromBytesToUuid(bytes));
    }

    @Test
    @DisplayName("UT fromBytesToUuid() when bytes length more than 16 should throw IAE")
    void fromBytesToUuid_whenBytesLengthMoreThan16_shouldThrow() {
        // given
        byte[] bytes = new byte[17];

        // when + then
        assertThrows(IllegalArgumentException.class, () -> tested.fromBytesToUuid(bytes));
    }

    @Test
    @DisplayName("UT fromBytesToUuid() when bytes is null should throw NPE")
    void fromBytesToUuid_whenBytesNull_shouldThrow() {
        // when + then
        assertThrows(NullPointerException.class, () -> tested.fromBytesToUuid(null));
    }
}
