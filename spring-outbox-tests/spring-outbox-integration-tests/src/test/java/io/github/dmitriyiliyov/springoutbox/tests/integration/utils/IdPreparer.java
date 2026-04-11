package io.github.dmitriyiliyov.springoutbox.tests.integration.utils;

import java.util.UUID;

@FunctionalInterface
public interface IdPreparer {
    Object prepare(UUID id);
}
