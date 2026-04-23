package io.github.dmitriyiliyov.springoutbox.core;

import java.time.Duration;

public interface AdaptivePollingPropertiesHolder {

    /**
     * The initial delay before the first processing.
     */
    Duration getInitialDelay();

    /**
     * The min fixed delay between subsequent processing operations.
     */
    Duration getMinFixedDelay();

    /**
     * The max fixed delay between subsequent processing operations.
     */
    Duration getMaxFixedDelay();

    /**
     * The multiplier for exponential backoff delay between subsequent involve.
     */
    Double getMultiplier();
}
