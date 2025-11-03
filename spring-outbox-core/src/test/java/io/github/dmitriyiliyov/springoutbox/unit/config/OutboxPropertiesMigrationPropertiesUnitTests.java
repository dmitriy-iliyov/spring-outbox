package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.MigrationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesMigrationPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() with default constructor should assign default values")
    public void initialize_defaultConstructor_assignsDefaults() {
        // given
        MigrationProperties migration = new MigrationProperties();

        // when
        migration.initialize();

        // then
        assertTrue(migration.isEnabled());
        assertEquals("classpath:db/migration/outbox", migration.getLocation());
        assertEquals("outbox_schema_history", migration.getTable());
    }

    @Test
    @DisplayName("UT initialize() with enabled=true and null fields should assign defaults")
    public void initialize_enabledTrueWithNull_assignsDefaults() {
        // given
        MigrationProperties migration = new MigrationProperties();
        migration.setEnabled(true);
        migration.setLocation(null);
        migration.setTable(null);

        // when
        migration.initialize();

        // then
        assertTrue(migration.isEnabled());
        assertEquals("classpath:db/migration/outbox", migration.getLocation());
        assertEquals("outbox_schema_history", migration.getTable());
    }

    @Test
    @DisplayName("UT initialize() with enabled=true and blank fields should assign defaults")
    public void initialize_enabledTrueWithBlank_assignsDefaults() {
        // given
        MigrationProperties migration = new MigrationProperties();
        migration.setEnabled(true);
        migration.setLocation("   ");
        migration.setTable("   ");

        // when
        migration.initialize();

        // then
        assertTrue(migration.isEnabled());
        assertEquals("classpath:db/migration/outbox", migration.getLocation());
        assertEquals("outbox_schema_history", migration.getTable());
    }

    @Test
    @DisplayName("UT initialize() with enabled=true and valid fields should keep assigned values")
    public void initialize_enabledTrueWithValidValues_keepsValues() {
        // given
        MigrationProperties migration = new MigrationProperties();
        migration.setEnabled(true);
        migration.setLocation("custom/location");
        migration.setTable("custom_table");

        // when
        migration.initialize();

        // then
        assertTrue(migration.isEnabled());
        assertEquals("custom/location", migration.getLocation());
        assertEquals("custom_table", migration.getTable());
    }

    @Test
    @DisplayName("UT initialize() with enabled=false should disable migration and set fields to null")
    public void initialize_enabledFalse_disablesAndSetsNulls() {
        // given
        MigrationProperties migration = new MigrationProperties();
        migration.setEnabled(false);
        migration.setLocation("some/location");
        migration.setTable("some_table");

        // when
        migration.initialize();

        // then
        assertFalse(migration.isEnabled());
        assertNull(migration.getLocation());
        assertNull(migration.getTable());
    }

    @Test
    @DisplayName("UT initialize() with enabled=null should treat as enabled and assign defaults")
    public void initialize_enabledNull_assignsDefaults() {
        // given
        MigrationProperties migration = new MigrationProperties();
        migration.setEnabled(null);

        // when
        migration.initialize();

        // then
        assertTrue(migration.isEnabled());
        assertEquals("classpath:db/migration/outbox", migration.getLocation());
        assertEquals("outbox_schema_history", migration.getTable());
    }

    @Test
    @DisplayName("UT equals() and hashCode() behave correctly for identical and different objects")
    public void equalsAndHashCode_correctBehavior() {
        // given
        MigrationProperties m1 = new MigrationProperties();
        MigrationProperties m2 = new MigrationProperties();
        MigrationProperties m3 = new MigrationProperties();
        m3.setEnabled(false);
        m3.initialize();

        // then
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertNotEquals(m1, m3);
        assertNotEquals(m1.hashCode(), m3.hashCode());
    }
}
