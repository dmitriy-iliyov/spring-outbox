package io.github.dmitriyiliyov.springoutbox.tests.integration;

import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.IdExtractor;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.MySqlIdExtractor;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.OracleIdExtractor;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.PostgresSqlIdExtractor;

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
