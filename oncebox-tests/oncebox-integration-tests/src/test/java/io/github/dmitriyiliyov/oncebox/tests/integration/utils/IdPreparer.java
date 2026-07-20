package io.github.dmitriyiliyov.oncebox.tests.integration.utils;

import java.util.UUID;

@FunctionalInterface
public interface IdPreparer {
    Object prepare(UUID id);
}
