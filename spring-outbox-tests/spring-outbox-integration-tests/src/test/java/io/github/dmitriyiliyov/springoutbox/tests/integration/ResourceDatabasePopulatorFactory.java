package io.github.dmitriyiliyov.springoutbox.tests.integration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.charset.StandardCharsets;

public final class ResourceDatabasePopulatorFactory {

    interface ResourceDatabasePopulatorFactorySupplier {
        ResourceDatabasePopulator supply();
    }

    private ResourceDatabasePopulatorFactory() {}

    public static ResourceDatabasePopulator generate(DatabaseType databaseType) {
        return switch (databaseType) {
            case MY_SQL -> new MySqlResourceDatabasePopulatorFactorySupplier().supply();
            case POSTGRES_SQL -> new PostgresResourceDatabasePopulatorFactorySupplier().supply();
            case ORACLE -> new OracleResourceDatabasePopulatorFactorySupplier().supply();
        };
    }

    private static class PostgresResourceDatabasePopulatorFactorySupplier implements ResourceDatabasePopulatorFactorySupplier {

        @Override
        public ResourceDatabasePopulator supply() {
            return new ResourceDatabasePopulator(
                    false,
                    false,
                    StandardCharsets.UTF_8.name(),
                    new ClassPathResource("psql/psql_business_table.sql")
            );
        }
    }

    private static class MySqlResourceDatabasePopulatorFactorySupplier implements ResourceDatabasePopulatorFactorySupplier {

        @Override
        public ResourceDatabasePopulator supply() {
            return new ResourceDatabasePopulator(
                    false,
                    false,
                    StandardCharsets.UTF_8.name(),
                    new ClassPathResource("mysql/mysql_business_table.sql")
            );
        }
    }

    private static class OracleResourceDatabasePopulatorFactorySupplier implements ResourceDatabasePopulatorFactorySupplier {

        @Override
        public ResourceDatabasePopulator supply() {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.setSeparator("/");
            populator.setScripts(
                    new ClassPathResource("oracle/oracle_business_table.sql")
            );
            populator.setContinueOnError(false);
            return populator;
        }
    }
}
