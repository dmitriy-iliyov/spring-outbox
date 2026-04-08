package io.github.dmitriyiliyov.springoutbox.tests.e2e.utils;

import java.util.UUID;

@FunctionalInterface
public interface IdPreparer {
    Object prepare(UUID id);
}
