package io.github.dmitriyiliyov.springoutbox.core.consumer;

public final class NoopConsumedOutboxCacheObserver implements ConsumedOutboxCacheObserver {

    public static final NoopConsumedOutboxCacheObserver INSTANCE = new NoopConsumedOutboxCacheObserver();

    private NoopConsumedOutboxCacheObserver() {}

    @Override
    public void onHit() {}

    @Override
    public void onMiss() {}
}
