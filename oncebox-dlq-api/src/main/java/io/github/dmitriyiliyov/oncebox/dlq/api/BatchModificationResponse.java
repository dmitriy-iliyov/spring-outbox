package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a batch operation over DLQ events.")
public record BatchModificationResponse(

        @Schema(
                description = "Total number of events requested for processing.",
                example = "100"
        )
        int requestedCount,

        @Schema(
                description = "Number of events successfully processed.",
                example = "95"
        )
        int processedCount,

        @Schema(
                description = "Overall status of the batch operation.",
                example = "PARTIAL_SUCCESS"
        )
        OperationStatus status,

        @Schema(
                description = "Human-readable explanation of the operation result.",
                example = "Some events were not updated because they were in IN_PROCESS status."
        )
        String message

) {

    public static BatchModificationResponse ofUpdate(int requestedCount, int actualUpdatedCount) {
        OperationStatus status;
        String message;
        if (requestedCount == actualUpdatedCount) {
            status = OperationStatus.SUCCESS;
            message = "All events were successfully updated.";
        } else {
            status = OperationStatus.PARTIAL_SUCCESS;
            message = "Some events were not updated because they were in IN_PROCESS status.";
        }
        return new BatchModificationResponse(requestedCount, actualUpdatedCount, status, message);
    }

    public static BatchModificationResponse ofUpdate(int actualUpdatedCount) {
        return new BatchModificationResponse(
                0,
                actualUpdatedCount,
                OperationStatus.POSSIBLE_PARTIAL_SUCCESS,
                "Some events might not have been updated because they were in IN_PROCESS status."
        );
    }

    public static BatchModificationResponse ofDelete(int requestedCount, int actualDeletedCount) {
        OperationStatus status;
        String message;
        if (requestedCount == actualDeletedCount) {
            status = OperationStatus.SUCCESS;
            message = "All events were successfully deleted.";
        } else {
            status = OperationStatus.PARTIAL_SUCCESS;
            message = "Some events were not deleted because they were in IN_PROCESS status.";
        }
        return new BatchModificationResponse(requestedCount, actualDeletedCount, status, message);
    }


    public static BatchModificationResponse ofDelete(int actualDeletedCount) {
        return new BatchModificationResponse(
                0,
                actualDeletedCount,
                OperationStatus.POSSIBLE_PARTIAL_SUCCESS,
                "Some events might not have been deleted because they were in IN_PROCESS status."
        );
    }
}
