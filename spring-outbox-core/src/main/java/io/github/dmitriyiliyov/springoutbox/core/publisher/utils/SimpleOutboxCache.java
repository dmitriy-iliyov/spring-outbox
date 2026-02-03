package io.github.dmitriyiliyov.springoutbox.core.publisher.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SimpleOutboxCache<S extends Enum<S>> implements OutboxCache<S> {

    private final CachedCount currentTotalCount;
    private final Duration COUNT_BY_STATUS_TTL;
    private final Map<S, CachedCount> countByStatus;
    private final Duration COUNT_BY_EVENT_TYPE_AND_STATUS_TTL;
    private static final String KEY_TEMPLATE = "%s:%s";
    private final Map<String, CachedCount> countByEventTypeAndStatus;

    public SimpleOutboxCache(long ... ttls) {
        if (ttls.length != 3) {
            throw new IllegalArgumentException("Ttls should contain three values");
        }
        this.currentTotalCount = new CachedCount(Duration.ofSeconds(ttls[0]));
        this.COUNT_BY_STATUS_TTL = Duration.ofSeconds(ttls[1]);
        this.countByStatus = new ConcurrentHashMap<>();
        this.COUNT_BY_EVENT_TYPE_AND_STATUS_TTL = Duration.ofSeconds(ttls[2]);
        this.countByEventTypeAndStatus = new ConcurrentHashMap<>();
    }

    @Override
    public Long getCount() {
        return currentTotalCount.getCount();
    }

    @Override
    public Long putCount(long count) {
        return currentTotalCount.setCount(count);
    }

    @Override
    public Long getCountByStatus(S status) {
        CachedCount cachedCount = countByStatus.get(status);
        if (cachedCount == null) {
            return null;
        }
        return cachedCount.getCount();
    }

    @Override
    public Long putCountByStatus(S status, long count) {
        return countByStatus.computeIfAbsent(status, k -> new CachedCount(COUNT_BY_STATUS_TTL)).setCount(count);
    }

    @Override
    public Long getCountByEventTypeAndStatus(String eventType, S status) {
        String key = KEY_TEMPLATE.formatted(eventType, status.name());
        CachedCount cachedCount = countByEventTypeAndStatus.get(key);
        if (cachedCount == null) {
            return null;
        }
        return cachedCount.getCount();
    }

    @Override
    public Long putCountByEventTypeAndStatus(String eventType, S status, long count) {
        String key = KEY_TEMPLATE.formatted(eventType, status.name());
        return countByEventTypeAndStatus
                .computeIfAbsent(key, k -> new CachedCount(COUNT_BY_EVENT_TYPE_AND_STATUS_TTL))
                .setCount(count);
    }

    protected static class CachedCount {

        private final Duration ttl;
        private Long count;
        private Instant cachedAt;

        private CachedCount(Duration ttl) {
            this.ttl = Objects.requireNonNull(ttl, "ttl cannot be null");
        }

        public synchronized Long setCount(long newCount) {
            count = newCount;
            cachedAt = Instant.now();
            return count;
        }

        public synchronized Long getCount() {
            if (count == null || cachedAt == null) {
                return null;
            }
            Duration alive = Duration.between(cachedAt, Instant.now());
            if (alive.compareTo(ttl) > 0) {
                return null;
            }
            return count;
        }
    }
}
