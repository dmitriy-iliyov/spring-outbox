package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Batch delete request for DLQ events")
public record BatchDeleteRequest(

        @Schema(
                description = "List of event ids to delete",
                example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maximum = "1000"
        )
        @Size(max = 1000, message = "Maximum 1000 ids allowed")
        Set<UUID> ids,

        @Schema(
                description = "Type of events to delete",
                example = "event-type",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String eventType

) {
        @AssertTrue(message = "Either ids or eventType must be provided, but not both")
        public boolean isValid() {
                return hasValidIds() ^ hasValidEventType();
        }

        public boolean hasValidIds() {
                return ids != null && !ids.isEmpty();
        }

        public boolean hasValidEventType() {
                return eventType != null && !eventType.isBlank();
        }
}
