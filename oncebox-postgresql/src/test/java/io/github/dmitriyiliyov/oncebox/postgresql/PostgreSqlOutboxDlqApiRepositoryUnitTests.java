package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlIdHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PostgreSqlOutboxDlqApiRepositoryUnitTests {

    @Test
    @DisplayName("UT constructor should throw NPE when jdbcTemplate is null")
    void constructor_shouldThrowNPE_whenJdbcTemplateIsNull() {
        SqlIdHelper idHelper = mock(SqlIdHelper.class);
        ResultSetMapper mapper = mock(ResultSetMapper.class);
        Clock clock = mock(Clock.class);

        assertThatThrownBy(() -> new PostgreSqlOutboxDlqApiRepository(null, idHelper, mapper, clock))
                .isInstanceOf(NullPointerException.class);
    }
}
