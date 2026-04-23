package io.github.dmitriyiliyov.springoutbox.core;

/**
 * Similar to {@link Runnable}, but returns a {@code boolean} indicating whether execution should continue.
 */
@FunctionalInterface
public interface Continuable {
    boolean run();
}
