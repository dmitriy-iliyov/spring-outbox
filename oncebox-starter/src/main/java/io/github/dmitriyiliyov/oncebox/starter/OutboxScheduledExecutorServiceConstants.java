package io.github.dmitriyiliyov.oncebox.starter;

import java.util.concurrent.TimeUnit;

/**
 * Constants used when shutting down the outbox ScheduledExecutorService.
 */
public final class OutboxScheduledExecutorServiceConstants {

    static final long TIMEOUT = 60;
    static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private OutboxScheduledExecutorServiceConstants() {}
}
