package io.github.dmitriyiliyov.springoutbox.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryUtilsUnitTests {

    @Test
    @DisplayName("UT isIdsValid() should return false when ids is empty")
    void isIdsValid_shouldReturnFalse_whenIdsIsEmpty() {
        // given
        Set<UUID> ids = Collections.emptySet();

        // when
        boolean result = RepositoryUtils.isIdsValid(ids);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("UT isIdsValid() should return false when ids size > 1000")
    void isIdsValid_shouldReturnFalse_whenIdsSizeIsTooLarge() {
        // given
        Set<UUID> ids = IntStream.range(0, 1001)
                .mapToObj(i -> UUID.randomUUID())
                .collect(Collectors.toSet());

        // when
        boolean result = RepositoryUtils.isIdsValid(ids);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("UT isIdsValid() should return true when ids size is valid")
    void isIdsValid_shouldReturnTrue_whenIdsSizeIsValid() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID());

        // when
        boolean result = RepositoryUtils.isIdsValid(ids);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("UT isIdsValid() should throw NPE when ids is null")
    void isIdsValid_shouldThrowNPE_whenIdsIsNull() {
        // given
        Set<UUID> ids = null;

        // when & then
        assertThatThrownBy(() -> RepositoryUtils.isIdsValid(ids))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ids cannot be null");
    }

    @Test
    @DisplayName("UT generateIdsPlaceholders() should return empty string when ids is empty")
    void generateIdsPlaceholders_shouldReturnEmptyString_whenIdsIsEmpty() {
        // given
        Set<UUID> ids = Collections.emptySet();

        // when
        String result = RepositoryUtils.generateIdsPlaceholders(ids);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT generateIdsPlaceholders() should return single placeholder when ids has 1 element")
    void generateIdsPlaceholders_shouldReturnSinglePlaceholder_whenIdsHasOneElement() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID());

        // when
        String result = RepositoryUtils.generateIdsPlaceholders(ids);

        // then
        assertThat(result).isEqualTo("?");
    }

    @Test
    @DisplayName("UT generateIdsPlaceholders() should return comma separated placeholders when ids has multiple elements")
    void generateIdsPlaceholders_shouldReturnCommaSeparatedPlaceholders_whenIdsHasMultipleElements() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());

        // when
        String result = RepositoryUtils.generateIdsPlaceholders(ids);

        // then
        assertThat(result).isEqualTo("?, ?");
    }

    @Test
    @DisplayName("UT generateValuesPlaceholders() should return empty string when tupleCount is 0")
    void generateValuesPlaceholders_shouldReturnEmptyString_whenTupleCountIsZero() {
        // given
        int tupleCount = 0;
        int valueCount = 1;

        // when
        String result = RepositoryUtils.generateValuesPlaceholders(tupleCount, valueCount);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT generateValuesPlaceholders() should return single tuple placeholder when tupleCount is 1")
    void generateValuesPlaceholders_shouldReturnSingleTuplePlaceholder_whenTupleCountIsOne() {
        // given
        int tupleCount = 1;
        int valueCount = 1;

        // when
        String result = RepositoryUtils.generateValuesPlaceholders(tupleCount, valueCount);

        // then
        assertThat(result).isEqualTo("(?)");
    }

    @Test
    @DisplayName("UT generateValuesPlaceholders() should return multiple tuple placeholders when tupleCount > 1")
    void generateValuesPlaceholders_shouldReturnMultipleTuplePlaceholders_whenTupleCountIsGreaterThanOne() {
        // given
        int tupleCount = 2;
        int valueCount = 1;

        // when
        String result = RepositoryUtils.generateValuesPlaceholders(tupleCount, valueCount);

        // then
        assertThat(result).isEqualTo("(?), (?)");
    }

    @Test
    @DisplayName("UT generateValuesPlaceholders() should handle valueCount correctly")
    void generateValuesPlaceholders_shouldHandleValueCountCorrectly() {
        // given
        int tupleCount = 2;
        int valueCount = 2;

        // when
        String result = RepositoryUtils.generateValuesPlaceholders(tupleCount, valueCount);

        // then
        assertThat(result).isEqualTo("(?,?), (?,?)");
    }
}
