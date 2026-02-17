package io.github.dmitriyiliyov.springoutbox.core.consumer;

public interface ConsumedOutboxCacheObserver {
    void onHit();
    void onMiss();
}
