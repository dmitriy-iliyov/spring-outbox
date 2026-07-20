package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchUpdateRequestUnitTests {

    @Test
    @DisplayName("UT isValid() when event type provided, should return true")
    void isValid_whenEventTypeProvided_shouldReturnTrue() {
        var request = new BatchUpdateRequest(null, "order-created", DlqStatus.MOVED);
        assertTrue(request.isValid());
    }

    @Test
    @DisplayName("UT isValid() when ids provided, should return true")
    void isValid_whenIdsProvided_shouldReturnTrue() {
        var request = new BatchUpdateRequest(Set.of(UUID.randomUUID()), null, DlqStatus.MOVED);
        assertTrue(request.isValid());
    }

    @Test
    @DisplayName("UT isValid() when both null, should return false")
    void isValid_whenBothNull_shouldReturnFalse() {
        var request = new BatchUpdateRequest(null, null, DlqStatus.MOVED);
        assertFalse(request.isValid());
    }

    @Test
    @DisplayName("UT isValid() when event type blank and ids null, should return false")
    void isValid_whenEventTypeBlankAndIdsNull_shouldReturnFalse() {
        var request = new BatchUpdateRequest(null, "   ", DlqStatus.MOVED);
        assertFalse(request.isValid());
    }

    @Test
    @DisplayName("UT isValid() when event type and ids passed, should return false")
    void isValid_whenEventTypeAndIdsPassed_shouldReturnFalse() {
        var request = new BatchUpdateRequest(Set.of(UUID.randomUUID()), "order-created", DlqStatus.MOVED);
        assertFalse(request.isValid());
    }

    @Test
    @DisplayName("UT isValid() when event type null and ids empty, should return false")
    void isValid_whenEventTypeNullAndIdsEmpty_shouldReturnFalse() {
        var request = new BatchUpdateRequest(Set.of(), null, DlqStatus.MOVED);
        assertFalse(request.isValid());
    }

    @Test
    @DisplayName("UT isValid() when event type blank and ids empty, should return false")
    void isValid_whenEventTypeBlankAndIdsEmpty_shouldReturnFalse() {
        var request = new BatchUpdateRequest(Set.of(), "   ", DlqStatus.MOVED);
        assertFalse(request.isValid());
    }
    
    @Test
    @DisplayName("UT hasValidEventType() when valid string, should return true")
    void hasValidEventType_whenValidString_shouldReturnTrue() {
        var request = new BatchUpdateRequest(null, "order-created", DlqStatus.MOVED);
        assertTrue(request.hasValidEventType());
    }

    @Test
    @DisplayName("UT hasValidEventType() when null, should return false")
    void hasValidEventType_whenNull_shouldReturnFalse() {
        var request = new BatchUpdateRequest(null, null, DlqStatus.MOVED);
        assertFalse(request.hasValidEventType());
    }

    @Test
    @DisplayName("UT hasValidEventType() when blank, should return false")
    void hasValidEventType_whenBlank_shouldReturnFalse() {
        var request = new BatchUpdateRequest(null, "   ", DlqStatus.MOVED);
        assertFalse(request.hasValidEventType());
    }
    
    @Test
    @DisplayName("UT hasValidIds() when non empty set, should return true")
    void hasValidIds_whenNonEmptySet_shouldReturnTrue() {
        var request = new BatchUpdateRequest(Set.of(UUID.randomUUID()), null, DlqStatus.MOVED);
        assertTrue(request.hasValidIds());
    }

    @Test
    @DisplayName("UT hasValidIds() when null, should return false")
    void hasValidIds_whenNull_shouldReturnFalse() {
        var request = new BatchUpdateRequest(null, null, DlqStatus.MOVED);
        assertFalse(request.hasValidIds());
    }

    @Test
    @DisplayName("UT hasValidIds() when empty set, should return false")
    void hasValidIds_whenEmptySet_shouldReturnFalse() {
        var request = new BatchUpdateRequest(Set.of(), null, DlqStatus.MOVED);
        assertFalse(request.hasValidIds());
    }
}
