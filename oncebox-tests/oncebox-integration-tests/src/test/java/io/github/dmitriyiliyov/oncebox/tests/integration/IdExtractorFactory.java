package io.github.dmitriyiliyov.oncebox.tests.integration;

import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.MySqlIdExtractor;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.OracleIdExtractor;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.PostgresSqlIdExtractor;

public final class IdExtractorFactory {

    private IdExtractorFactory() {}

    public static IdExtractor generate(DatabaseType databaseType) {
        return switch (databaseType) {
            case MY_SQL -> new MySqlIdExtractor();
            case POSTGRES_SQL -> new PostgresSqlIdExtractor();
            case ORACLE -> new OracleIdExtractor();
        };
    }
}
