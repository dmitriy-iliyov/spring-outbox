package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;

public class NoopContinuableTaskDecoratorSupplier implements ContinuableTaskDecoratorSupplier {
    @Override
    public ContinuableTaskDecorator supply(String taskType) {
        return ContinuableTaskDecorator.identity();
    }
}
