package io.github.dmitriyiliyov.springoutbox.core;

public interface OutboxCache<S extends Enum<S>> {
    Long getCount();

    Long putCount(long count);

    Long getCountByStatus(S status);

    Long putCountByStatus(S status, long count);

    Long getCountByEventTypeAndStatus(String eventType, S status);

    Long putCountByEventTypeAndStatus(String eventType, S status, long count);
}
