package com.nirvaha.nirvaha_backend;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class DatabaseMigrationLifecycleTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    private static final String[] EXPECTED_TABLES = {
            "administrative_areas",
            "anomaly_flags",
            "data_sources",
            "evidence",
            "incident_actions",
            "incident_feedbacks",
            "incident_service_tracking",
            "incidents",
            "intelligence_signals",
            "notifications",
            "officer_decisions",
            "password_reset_tokens",
            "resolutions",
            "risk_assessments",
            "road_segments",
            "service_rules",
            "users",
            "wards",
            "weather_observations"
    };

    @Test
    @DisplayName("Flyway bean is configured and migration info reflects baseline or applied V1")
    void testFlywayMigrationState() {
        assertNotNull(flyway, "Flyway bean must be configured and available in Spring context");
        MigrationInfo current = flyway.info().current();
        assertNotNull(current, "Current migration info must not be null");
        assertEquals("1", current.getVersion().getVersion(), "Current schema version must be 1");
    }

    @Test
    @DisplayName("Second migration execution is idempotent and executes zero additional migrations")
    void testSecondMigrationExecutionIsIdempotent() {
        int executedMigrations = flyway.migrate().migrationsExecuted;
        assertEquals(0, executedMigrations, "Subsequent migration call must execute 0 migrations (idempotent no-op)");
    }

    @Test
    @DisplayName("All 19 core platform tables exist and are verified in database catalog")
    void testAllExpectedTablesExistInDatabase() throws Exception {
        Set<String> actualTables = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                actualTables.add(rs.getString("TABLE_NAME"));
            }
        }

        for (String expectedTable : EXPECTED_TABLES) {
            assertTrue(actualTables.contains(expectedTable),
                    "Database must contain table: " + expectedTable);
        }
    }

    @Test
    @DisplayName("Flyway validate checks classpath migrations against schema history without errors")
    void testFlywayValidationPasses() {
        assertDoesNotThrow(() -> flyway.validate(),
                "Flyway validate must pass without throwing validation errors");
    }
}
