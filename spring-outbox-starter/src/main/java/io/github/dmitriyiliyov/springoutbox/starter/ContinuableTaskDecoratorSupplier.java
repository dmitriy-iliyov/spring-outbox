package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;

public interface ContinuableTaskDecoratorSupplier {
    ContinuableTaskDecorator supply(String taskType);
}
