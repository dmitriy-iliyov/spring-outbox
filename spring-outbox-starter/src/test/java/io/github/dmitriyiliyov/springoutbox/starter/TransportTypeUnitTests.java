package io.github.dmitriyiliyov.springoutbox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransportTypeUnitTests {

    @Test
    @DisplayName("UT fromString() when value is null, should throws")
    public void fromString_whenValueIsNull_shouldThrows() {
        // given
        String value = null;

        // when + then
        assertThrows(IllegalArgumentException.class, () -> TransportType.fromString(value));
    }

    @Test
    @DisplayName("UT fromString() when value valid, should return SenderType")
    public void fromString_whenValueIsNull_shouldReturnSenderType() {
        // given
        String value = "kaFKa";

        // when
        TransportType type = TransportType.fromString(value);

        // then
        assertEquals(TransportType.KAFKA, type);
    }

    @Test
    @DisplayName("UT fromString() when value represent unknown senderType, should throws")
    public void fromString_whenValueIsUnknown_shouldThrows() {
        // given
        String value = "UNKNOWN_SENDER_TYPE";

        // when + then
        assertThrows(IllegalArgumentException.class, () -> TransportType.fromString(value));
    }
}
