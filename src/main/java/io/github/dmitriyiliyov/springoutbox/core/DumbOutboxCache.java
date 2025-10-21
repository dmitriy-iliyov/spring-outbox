package io.github.dmitriyiliyov.springoutbox.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DumOutboxCache<S extends Enum<S>> implements OutboxCache<S> {

    private final CachedCount totalCount;
    private final Duration COUNT_BY_STATUS_TTL;
    private final Map<S, CachedCount> countByStatus;
    private final Duration COUNT_BY_EVENT_TYPE_AND_STATUS_TTL;
    private static final String KEY_TEMPLATE = "%s:%s";
    private final Map<String, CachedCount> countByEventTypeAndStatus;

    public DumOutboxCache(long ... ttlSecs) {
        this.totalCount = new CachedCount(Duration.ofSeconds(ttlSecs[0]));
        this.COUNT_BY_STATUS_TTL = Duration.ofSeconds(ttlSecs[1]);
        this.countByStatus = new ConcurrentHashMap<>();
        this.COUNT_BY_EVENT_TYPE_AND_STATUS_TTL = Duration.ofSeconds(ttlSecs[2]);
        this.countByEventTypeAndStatus = new ConcurrentHashMap<>();
    }

    @Override
    public Long getCount() {
        return totalCount.getCount();
    }

    @Override
    public Long putCount(long count) {
        return totalCount.setCount(count);
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

    private static class CachedCount {

        private final Duration ttl;
        private Long count;
        private Instant cachedAt;

        private CachedCount(Duration ttl) {
            this.ttl = ttl;
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
