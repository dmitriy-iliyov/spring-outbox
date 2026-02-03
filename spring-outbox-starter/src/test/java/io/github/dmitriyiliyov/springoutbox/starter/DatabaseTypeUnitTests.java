package io.github.dmitriyiliyov.springoutbox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseTypeUnitTests {

    private static final String INVALID_INPUT_MESSAGE = "DatabaseType is null, empty or blank";

    @Test
    @DisplayName("UT fromString(), should throws when value is null")
    public void fromString_whenValueIsNull_shouldThrows() {
        // given
        String value = null;

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString(value));

        // then
        assertEquals(e.getMessage(), INVALID_INPUT_MESSAGE);
    }

    @Test
    @DisplayName("UT fromString(), should throws when value is empty")
    public void fromString_whenValueIsEmpty_shouldThrows() {
        // given
        String value = "";

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString(value));

        // then
        assertEquals(e.getMessage(), INVALID_INPUT_MESSAGE);
    }

    @Test
    @DisplayName("UT fromString(), should throws when value is blank")
    public void fromString_whenValueIsBlank_shouldThrows() {
        // given
        String value = "    ";

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString(value));

        // then
        assertEquals(e.getMessage(), INVALID_INPUT_MESSAGE);
    }
}
