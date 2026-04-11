package io.github.dmitriyiliyov.springoutbox.tests.integration;

public enum DatabaseType {

    POSTGRES_SQL(
            "org.postgresql.Driver"
    ),
    MY_SQL(
            "com.mysql.cj.jdbc.Driver"
    ),
    ORACLE(
            "oracle.jdbc.OracleDriver"
    );

    private final String driverClassName;

    DatabaseType(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }
}
