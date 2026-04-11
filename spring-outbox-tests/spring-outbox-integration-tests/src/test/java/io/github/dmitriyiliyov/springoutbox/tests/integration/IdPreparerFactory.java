package io.github.dmitriyiliyov.springoutbox.tests.integration;

import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.IdPreparer;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.MySqlIdPreparer;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.OracleIdPreparer;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.PostgresSqlIdPreparer;

public final class IdPreparerFactory {

    private IdPreparerFactory() {}

    public static IdPreparer generate(DatabaseType databaseType) {
        return switch (databaseType) {
            case MY_SQL -> new MySqlIdPreparer();
            case POSTGRES_SQL -> new PostgresSqlIdPreparer();
            case ORACLE -> new OracleIdPreparer();
        };
    }
}
