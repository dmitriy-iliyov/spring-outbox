package io.github.dmitriyiliyov.springoutbox.dlq.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchModificationResponseUnitTests {

    @Test
    @DisplayName("UT ofUpdate() when requested matches actual updated count should return SUCCESS")
    void ofUpdate_whenRequestedEqualsActual_shouldReturnSuccess() {
        // given
        int requestedCount = 5;
        int actualUpdatedCount = 5;

        // when
        BatchModificationResponse response = BatchModificationResponse.ofUpdate(requestedCount, actualUpdatedCount);

        // then
        assertEquals(5, response.requestedCount());
        assertEquals(5, response.processedCount());
        assertEquals(OperationStatus.SUCCESS, response.status());
        assertEquals("All events were successfully updated.", response.message());
    }

    @Test
    @DisplayName("UT ofUpdate() when actual updated count is less than requested should return PARTIAL_SUCCESS")
    void ofUpdate_whenRequestedDoesNotEqualActual_shouldReturnPartialSuccess() {
        // given
        int requestedCount = 5;
        int actualUpdatedCount = 3;

        // when
        BatchModificationResponse response = BatchModificationResponse.ofUpdate(requestedCount, actualUpdatedCount);

        // then
        assertEquals(5, response.requestedCount());
        assertEquals(3, response.processedCount());
        assertEquals(OperationStatus.PARTIAL_SUCCESS, response.status());
        assertEquals("Some events were not updated because they were in IN_PROCESS status.", response.message());
    }

    @Test
    @DisplayName("UT ofUpdate() when both requested and actual are 0 should return SUCCESS")
    void ofUpdate_whenZeroRequestedAndActual_shouldReturnSuccess() {
        // given
        int requestedCount = 0;
        int actualUpdatedCount = 0;

        // when
        BatchModificationResponse response = BatchModificationResponse.ofUpdate(requestedCount, actualUpdatedCount);

        // then
        assertEquals(0, response.requestedCount());
        assertEquals(0, response.processedCount());
        assertEquals(OperationStatus.SUCCESS, response.status());
        assertEquals("All events were successfully updated.", response.message());
    }

    @Test
    @DisplayName("UT ofUpdate() when 0 events were updated out of requested should return PARTIAL_SUCCESS")
    void ofUpdate_whenActualIsZero_shouldReturnPartialSuccess() {
        // given
        int requestedCount = 5;
        int actualUpdatedCount = 0;

        // when
        BatchModificationResponse response = BatchModificationResponse.ofUpdate(requestedCount, actualUpdatedCount);

        // then
        assertEquals(5, response.requestedCount());
        assertEquals(0, response.processedCount());
        assertEquals(OperationStatus.PARTIAL_SUCCESS, response.status());
        assertEquals("Some events were not updated because they were in IN_PROCESS status.", response.message());
    }
}
