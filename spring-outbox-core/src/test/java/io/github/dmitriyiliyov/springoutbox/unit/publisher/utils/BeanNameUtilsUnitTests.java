package io.github.dmitriyiliyov.springoutbox.unit.publisher.utils;

import io.github.dmitriyiliyov.springoutbox.publisher.utils.BeanNameUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BeanNameUtilsUnitTests {

    @Test
    @DisplayName("UT toBeanName(), should return correct bean name for normal string")
    public void toBeanName_shouldReturnCorrectBeanName() {
        String result = BeanNameUtils.toBeanName("TestEvent", "Scheduler");
        assertEquals("testEventScheduler", result);
    }

    @Test
    @DisplayName("UT toBeanName(), should remove special characters")
    public void toBeanName_shouldRemoveSpecialCharacters() {
        String result = BeanNameUtils.toBeanName("Test@#Event!$", "Bean");
        assertEquals("testEventBean", result);
    }

    @Test
    @DisplayName("UT toBeanName(), should trim leading/trailing spaces")
    public void toBeanName_shouldTrimSpaces() {
        String result = BeanNameUtils.toBeanName("  MyEvent  ", "Bean");
        assertEquals("myEventBean", result);
    }

    @Test
    @DisplayName("UT toBeanName(), should default to defaultEvent when string empty after cleaning")
    public void toBeanName_shouldDefaultWhenEmpty() {
        String result = BeanNameUtils.toBeanName("@#$%", "Bean");
        assertEquals("defaultEventBean", result);
    }

    @Test
    @DisplayName("UT toBeanName(), should throw IllegalArgumentException when null or blank")
    public void toBeanName_shouldThrowExceptionWhenNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> BeanNameUtils.toBeanName(null, "Bean"));
        assertThrows(IllegalArgumentException.class, () -> BeanNameUtils.toBeanName("", "Bean"));
        assertThrows(IllegalArgumentException.class, () -> BeanNameUtils.toBeanName("   ", "Bean"));
    }
}
