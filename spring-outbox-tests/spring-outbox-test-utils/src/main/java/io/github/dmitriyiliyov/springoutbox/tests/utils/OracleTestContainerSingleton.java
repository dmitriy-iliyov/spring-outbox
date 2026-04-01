package io.github.dmitriyiliyov.springoutbox.tests.utils;

import org.testcontainers.oracle.OracleContainer;

public class OracleTestContainerSingleton {

    public static final OracleContainer INSTANCE;

    static {
        INSTANCE = new OracleContainer("gvenzl/oracle-free:23")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        INSTANCE.start();
    }

    private OracleTestContainerSingleton() {}
}