package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.MigrationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesMigrationPropertiesUnitTests {

    @Test
    @DisplayName("UT default constructor should assign defaults")
    public void constructor_default_shouldAssignDefaults() {
        // given + when
        MigrationProperties migration = new MigrationProperties();

        // then
        assertTrue(migration.isEnabled());
        assertEquals("classpath:db/migration/outbox", migration.getLocation());
        assertEquals("outbox_schema_history", migration.getTable());
    }

    @Test
    @DisplayName("UT constructor with enabled = true and null parameters should assign defaults")
    public void constructor_enabledTrueNullParameters_shouldAssignDefaults() {
        // given + when
        MigrationProperties migration = new MigrationProperties(true, null, null);

        // then
        assertTrue(migration.isEnabled());
        assertEquals("classpath:db/migration/outbox", migration.getLocation());
        assertEquals("outbox_schema_history", migration.getTable());
    }

    @Test
    @DisplayName("UT constructor with enabled = true and blank parameters should assign defaults")
    public void constructor_enabledTrueBlankParameters_shouldAssignDefaults() {
        // given + when
        MigrationProperties migration = new MigrationProperties(true, "   ", "   ");

        // then
        assertTrue(migration.isEnabled());
        assertEquals("classpath:db/migration/outbox", migration.getLocation());
        assertEquals("outbox_schema_history", migration.getTable());
    }

    @Test
    @DisplayName("UT constructor with enabled = true and valid parameters should assign values")
    public void constructor_enabledTrueValidParameters_shouldAssignValues() {
        // given
        String location = "custom/location";
        String table = "custom_table";

        // when
        MigrationProperties migration = new MigrationProperties(true, location, table);

        // then
        assertTrue(migration.isEnabled());
        assertEquals(location, migration.getLocation());
        assertEquals(table, migration.getTable());
    }

    @Test
    @DisplayName("UT constructor with enabled = false should disable migration and set nulls")
    public void constructor_enabledFalse_shouldDisableAndSetNulls() {
        // given + when
        MigrationProperties migration = new MigrationProperties(false, "some/location", "some_table");

        // then
        assertFalse(migration.isEnabled());
        assertNull(migration.getLocation());
        assertNull(migration.getTable());
    }

    @Test
    @DisplayName("UT constructor with enabled = null should treat as enabled and assign defaults")
    public void constructor_enabledNull_shouldTreatAsEnabled() {
        // given + when
        MigrationProperties migration = new MigrationProperties(null, null, null);

        // then
        assertTrue(migration.isEnabled());
        assertEquals("classpath:db/migration/outbox", migration.getLocation());
        assertEquals("outbox_schema_history", migration.getTable());
    }

    @Test
    @DisplayName("UT equals and hashCode should behave correctly")
    public void equalsAndHashCode_shouldBehaveCorrectly() {
        // given
        MigrationProperties m1 = new MigrationProperties();
        MigrationProperties m2 = new MigrationProperties();
        MigrationProperties m3 = new MigrationProperties(false, null, null);

        // then
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertNotEquals(m1, m3);
        assertNotEquals(m1.hashCode(), m3.hashCode());
    }
}
