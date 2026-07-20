package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;

/**
 * A supplier for providing {@link ContinuableTaskDecorator} instances.
 */
public interface ContinuableTaskDecoratorSupplier {
    
    /**
     * Supplies a {@link ContinuableTaskDecorator} based on the specified task type.
     *
     * @param taskType the type of task for which to provide the decorator.
     * @return a configured {@link ContinuableTaskDecorator}.
     */
    ContinuableTaskDecorator supply(String taskType);
}
