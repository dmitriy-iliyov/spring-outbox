package io.github.dmitriyiliyov.oncebox.tests.integration.utils;

import java.util.UUID;

public class PostgresSqlIdPreparer implements IdPreparer {
    @Override
    public Object prepare(UUID id) {
        return id;
    }
}
