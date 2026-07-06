package io.github.dmitriyiliyov.springoutbox.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SetUtilsUnitTests {

    @Test
    @DisplayName("UT isEmpty() when set is null should return true")
    void isEmpty_whenSetIsNull_shouldReturnTrue() {
        // given
        Set<String> set = null;

        // when
        boolean result = SetUtils.isEmpty(set);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("UT isEmpty() when set is empty should return true")
    void isEmpty_whenSetIsEmpty_shouldReturnTrue() {
        // given
        Set<String> set = Collections.emptySet();

        // when
        boolean result = SetUtils.isEmpty(set);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("UT isEmpty() when set has elements should return false")
    void isEmpty_whenSetHasElements_shouldReturnFalse() {
        // given
        Set<String> set = Set.of("element");

        // when
        boolean result = SetUtils.isEmpty(set);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("UT mutableCopy() when set is null should return null")
    void mutableCopy_whenSetIsNull_shouldReturnNull() {
        // given
        Set<String> set = null;

        // when
        Set<String> result = SetUtils.mutableCopy(set);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("UT mutableCopy() when set is empty should return empty mutable set")
    void mutableCopy_whenSetIsEmpty_shouldReturnEmptyMutableSet() {
        // given
        Set<String> set = Set.of();

        // when
        Set<String> result = SetUtils.mutableCopy(set);

        // then
        assertThat(result).isEmpty();

        assertDoesNotThrow(() -> result.add("new-element"));
        assertThat(result).containsExactly("new-element");
    }

    @Test
    @DisplayName("UT mutableCopy() when set has elements should return mutable copy with same elements")
    void mutableCopy_whenSetHasElements_shouldReturnMutableCopy() {
        // given
        Set<String> set = Set.of("element1", "element2");

        // when
        Set<String> result = SetUtils.mutableCopy(set);

        // then
        assertThat(result).containsExactlyInAnyOrder("element1", "element2");

        assertDoesNotThrow(() -> result.add("element3"));
        assertThat(result).containsExactlyInAnyOrder("element1", "element2", "element3");
    }
}