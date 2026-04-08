package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.config;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.JdbcConsumerBusinessRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.ByteBuffer;

@TestConfiguration
@Profile("oracle-e2e & consume-e2e")
public class OracleE2eTestsConfig {

    @Bean
    public DataSourceInitializer oracleOutboxDataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSeparator("/");
        populator.setScripts(
                new ClassPathResource("oracle/oracle_business_table.sql")
        );
        populator.setContinueOnError(false);
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    @Bean
    public ConsumerBusinessRepository jdbcConsumerBusinessRepository(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new JdbcConsumerBusinessRepository(
                jdbcTemplate,
                id -> {
                    ByteBuffer bb = ByteBuffer.allocate(16);
                    bb.putLong(id.getMostSignificantBits());
                    bb.putLong(id.getLeastSignificantBits());
                    return bb.array();
                }
        );
    }
}
