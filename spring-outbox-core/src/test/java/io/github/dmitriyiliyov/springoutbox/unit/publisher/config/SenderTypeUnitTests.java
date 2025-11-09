package io.github.dmitriyiliyov.springoutbox.unit.publisher.config;

import io.github.dmitriyiliyov.springoutbox.publisher.config.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SenderTypeUnitTests {

    @Test
    @DisplayName("UT fromString() when value is null, should throws")
    public void fromString_whenValueIsNull_shouldThrows() {
        // given
        String value = null;

        // when + then
        assertThrows(IllegalArgumentException.class, () -> SenderType.fromString(value));
    }

    @Test
    @DisplayName("UT fromString() when value valid, should return SenderType")
    public void fromString_whenValueIsNull_shouldReturnSenderType() {
        // given
        String value = "kaFKa";

        // when
        SenderType type = SenderType.fromString(value);

        // then
        assertEquals(SenderType.KAFKA, type);
    }

    @Test
    @DisplayName("UT fromString() when value represent unknown senderType, should throws")
    public void fromString_whenValueIsUnknown_shouldThrows() {
        // given
        String value = "UNKNOWN_SENDER_TYPE";

        // when + then
        assertThrows(IllegalArgumentException.class, () -> SenderType.fromString(value));
    }
}
