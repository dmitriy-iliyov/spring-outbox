package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;

public final class NoopContinuableTaskDecoratorSupplier implements ContinuableTaskDecoratorSupplier {
    @Override
    public ContinuableTaskDecorator supply(String taskType) {
        return ContinuableTaskDecorator.identity();
    }
}
