package io.github.dmitriyiliyov.oncebox.core;

/**
 * Similar to {@link Runnable}, but returns a {@code boolean} indicating whether execution should continue.
 */
@FunctionalInterface
public interface ContinuableTask {
    boolean run();
}
