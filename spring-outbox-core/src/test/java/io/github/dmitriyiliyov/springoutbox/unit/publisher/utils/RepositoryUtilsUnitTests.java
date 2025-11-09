package io.github.dmitriyiliyov.springoutbox.unit.publisher.utils;

import io.github.dmitriyiliyov.springoutbox.publisher.utils.RepositoryUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RepositoryUtilsUnitTests {


    @Test
    @DisplayName("UT validateIds() when ids is null, should throws")
    public void validateIds_whenIdsIsNull_shouldThrows() {
        // given
        Set<UUID> ids = null;

        // when + then
        assertThrows(NullPointerException.class, () -> RepositoryUtils.validateIds(ids));
    }

    @Test
    @DisplayName("UT validateIds() when ids is empty, should return false")
    public void validateIds_whenIdsIsEmpty_shouldReturnFalse() {
        // given
        Set<UUID> ids = Set.of();

        // when
        boolean result = RepositoryUtils.validateIds(ids);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT validateIds() when ids is to large, should return false")
    public void validateIds_whenIdsIsToLarge_shouldReturnFalse() {
        // given
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 101; i++) {
            ids.add(UUID.randomUUID());
        }

        // when
        boolean result = RepositoryUtils.validateIds(ids);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT generatePlaceholders(), should return placeholder")
    public void generatePlaceholders_shouldGeneratePlaceholders() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        // when
        String result = RepositoryUtils.generatePlaceholders(ids);

        // then
        assertFalse(result.isBlank());
        assertEquals("?, ?, ?", result);
        assertEquals(ids.size(), result.split(", ").length);
    }
}
