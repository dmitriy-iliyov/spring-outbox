package io.github.dmitriyiliyov.oncebox.core;

/**
 * A strategy for decorating a {@link ContinuableTask}, allowing additional behavior to be wrapped around its execution.
 */
public interface ContinuableTaskDecorator {

    /**
     * Decorates the given task with additional behavior.
     *
     * @param task the original task to be decorated.
     * @return the decorated task.
     */
    ContinuableTask decorate(ContinuableTask task);

    /**
     * Returns a decorator that simply returns the original task without modifications.
     *
     * @return an identity decorator.
     */
    static ContinuableTaskDecorator identity() {
        return task -> task;
    }
}
