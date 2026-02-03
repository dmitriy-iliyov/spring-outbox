package io.github.dmitriyiliyov.springoutbox.starter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

public final class JdbcTemplateFactory {
    public static JdbcTemplate getSynchronizedJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(new TransactionAwareDataSourceProxy(dataSource));
    }
}
