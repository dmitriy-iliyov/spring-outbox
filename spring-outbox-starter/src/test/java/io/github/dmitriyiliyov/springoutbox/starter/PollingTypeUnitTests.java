package io.github.dmitriyiliyov.springoutbox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PollingTypeUnitTests {

    private static final String INVALID_INPUT_MESSAGE = "PoolingType is null, empty or blank";

    @Test
    @DisplayName("UT from(), should throws when value is null")
    public void from_whenValueIsNull_shouldThrows() {
        // given
        String value = null;

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PollingType.from(value));

        // then
        assertEquals(INVALID_INPUT_MESSAGE, e.getMessage());
    }

    @Test
    @DisplayName("UT from(), should throws when value is empty")
    public void from_whenValueIsEmpty_shouldThrows() {
        // given
        String value = "";

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PollingType.from(value));

        // then
        assertEquals(INVALID_INPUT_MESSAGE, e.getMessage());
    }

    @Test
    @DisplayName("UT from(), should throws when value is blank")
    public void from_whenValueIsBlank_shouldThrows() {
        // given
        String value = "    ";

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PollingType.from(value));

        // then
        assertEquals(INVALID_INPUT_MESSAGE, e.getMessage());
    }

    @Test
    @DisplayName("UT from(), should throws when value is unsupported")
    public void from_whenValueIsUnsupported_shouldThrows() {
        // given
        String value = "RANDOM_TYPE";
        String expectedMessage = "Unsupported pooling type 'RANDOM_TYPE'";

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PollingType.from(value));

        // then
        assertEquals(expectedMessage, e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fixed", "FIXED", "fIxEd"})
    @DisplayName("UT from(), should return FIXED regardless of case")
    public void from_whenValueIsFixed_shouldReturnEnum(String value) {
        // when
        PollingType result = PollingType.from(value);

        // then
        assertEquals(PollingType.FIXED, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"adaptive", "ADAPTIVE", "AdApTiVe"})
    @DisplayName("UT from(), should return ADAPTIVE regardless of case")
    public void from_whenValueIsAdaptive_shouldReturnEnum(String value) {
        // when
        PollingType result = PollingType.from(value);

        // then
        assertEquals(PollingType.ADAPTIVE, result);
    }
}
