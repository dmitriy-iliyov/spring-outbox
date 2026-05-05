package io.github.dmitriyiliyov.springoutbox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OnDatabaseTypeConditionIntegrationTests {

    @Configuration
    static class TestConfiguration {

        @Bean
        @ConditionalOnDatabaseType(type = DatabaseType.POSTGRESQL)
        String postgresBean() {
            return "postgres-bean";
        }

        @Bean
        @ConditionalOnDatabaseType(type = DatabaseType.MYSQL)
        String mysqlBean() {
            return "mysql-bean";
        }

        @Bean
        @ConditionalOnDatabaseType(type = DatabaseType.ORACLE)
        String oracleBean() {
            return "oracle-bean";
        }
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class);
    }

    @Test
    @DisplayName("IT should match postgres bean when jdbc url is postgresql")
    void shouldMatch_whenJdbcUrlIsPostgresql() {
        baseRunner()
                .withPropertyValues("spring.datasource.url=jdbc:postgresql://localhost:5432/testdb")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("postgresBean");
                    assertThat(context).doesNotHaveBean("mysqlBean");
                    assertThat(context).doesNotHaveBean("oracleBean");
                });
    }

    @Test
    @DisplayName("IT should match mysql bean when jdbc url is mysql")
    void shouldMatch_whenJdbcUrlIsMysql() {
        baseRunner()
                .withPropertyValues("spring.datasource.url=jdbc:mysql://localhost:3306/testdb")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("mysqlBean");
                    assertThat(context).doesNotHaveBean("postgresBean");
                    assertThat(context).doesNotHaveBean("oracleBean");
                });
    }

    @Test
    @DisplayName("IT should match oracle bean when jdbc url is oracle")
    void shouldMatch_whenJdbcUrlIsOracle() {
        baseRunner()
                .withPropertyValues("spring.datasource.url=jdbc:oracle:thin:@localhost:1521:orcl")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("oracleBean");
                    assertThat(context).doesNotHaveBean("postgresBean");
                    assertThat(context).doesNotHaveBean("mysqlBean");
                });
    }

    @Test
    @DisplayName("IT should not match any bean when jdbc url is unknown")
    void shouldNotMatch_whenJdbcUrlIsUnknown() {
        baseRunner()
                .withPropertyValues("spring.datasource.url=jdbc:unknown://localhost:1234/testdb")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("postgresBean");
                    assertThat(context).doesNotHaveBean("mysqlBean");
                    assertThat(context).doesNotHaveBean("oracleBean");
                });
    }

    @Test
    @DisplayName("IT should not match any bean when jdbc url is not provided")
    void shouldNotMatch_whenJdbcUrlIsAbsent() {
        baseRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("postgresBean");
                    assertThat(context).doesNotHaveBean("mysqlBean");
                    assertThat(context).doesNotHaveBean("oracleBean");
                });
    }
}