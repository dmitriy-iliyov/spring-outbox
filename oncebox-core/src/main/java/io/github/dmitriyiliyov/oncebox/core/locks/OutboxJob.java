package io.github.dmitriyiliyov.oncebox.core.locks;

public enum OutboxJob {
    OUTBOX_PROCESSED_CLEANUP("outbox-processed-cleanup"),
    OUTBOX_DLQ_CLEANUP("outbox-dlq-cleanup"),
    OUTBOX_CONSUMED_CLEANUP("outbox-consumed-cleanup");

    private final String jobName;

    OutboxJob(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }
}
