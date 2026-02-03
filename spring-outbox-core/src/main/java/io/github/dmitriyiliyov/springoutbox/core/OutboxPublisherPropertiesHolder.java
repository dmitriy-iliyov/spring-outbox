package io.github.dmitriyiliyov.springoutbox.core;

import java.time.Duration;
import java.util.Map;

public interface OutboxPublisherPropertiesHolder {

    boolean existEventType(String eventType);

    Map<String, EventPropertiesHolder> getEventHolders();

    interface StuckRecoveryPropertiesHolder {

        Duration getMaxBatchProcessingTime();

        Integer getBatchSize();

        Duration getInitialDelay();

        Duration getFixedDelay();
    }

    interface EventPropertiesHolder {

        String getEventType();

        Duration getInitialDelay();

        Duration getFixedDelay();

        Integer getBatchSize();

        String getTopic();

        Integer getMaxRetries();

        long backoffMultiplier();

        long backoffDelay();
    }

    interface DlqPropertiesHolder {
        Integer getBatchSize();

        Duration getTransferToInitialDelay();

        Duration getTransferToFixedDelay();

        Duration getTransferFromInitialDelay();

        Duration getTransferFromFixedDelay();
    }
}
