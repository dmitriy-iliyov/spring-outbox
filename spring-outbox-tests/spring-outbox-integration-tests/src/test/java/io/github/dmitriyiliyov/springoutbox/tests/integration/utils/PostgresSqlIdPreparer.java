package io.github.dmitriyiliyov.springoutbox.tests.integration.utils;

import java.util.UUID;

public class PostgresSqlIdPreparer implements IdPreparer {
    @Override
    public Object prepare(UUID id) {
        return id;
    }
}
