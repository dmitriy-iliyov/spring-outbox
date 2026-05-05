package io.github.dmitriyiliyov.springoutbox.metrics;

public enum MetricsTaskType {
    PUBLISHER_CLEANUP("cleanup-processed-events"),
    CONSUMER_CLEANUP("cleanup-consumed-events"),
    STUCK_RECOVERY("stuck-event-recovery"),
    TRANSFER_TO_DLQ("transfer-to-dlq"),
    TRANSFER_FROM_DLQ("transfer-from-dlq"),
    DLQ_CLEANUP("cleanup-resolved-dlq-events");

    private final String value;

    MetricsTaskType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
