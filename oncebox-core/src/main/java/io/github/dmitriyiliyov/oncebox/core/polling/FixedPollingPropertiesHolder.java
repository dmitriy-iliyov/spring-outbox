package io.github.dmitriyiliyov.oncebox.core.polling;

import java.time.Duration;

public interface FixedPollingPropertiesHolder {

    /**
     * The initial delay before the first processing.
     */
    Duration getInitialDelay();

    /**
     * The fixed delay between subsequent processing operations.
     */
    Duration getFixedDelay();
}

