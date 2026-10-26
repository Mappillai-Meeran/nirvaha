package com.nirvaha.nirvaha_backend.safeguard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseIsolationTestSafeguardTest {

    @Test
    @DisplayName("Should detect development database nirvaha_db from catalog or url")
    void shouldDetectDevelopmentDatabase() {
        // Forbidden catalog
        assertTrue(DatabaseIsolationTestSafeguard.isForbiddenDevDatabase("nirvaha_db", "jdbc:postgresql://localhost:5432/nirvaha_db"));
        assertTrue(DatabaseIsolationTestSafeguard.isForbiddenDevDatabase("NIRVAHA_DB", "jdbc:postgresql://localhost:5432/some_other_name"));
        assertTrue(DatabaseIsolationTestSafeguard.isForbiddenDevDatabase("other", "jdbc:postgresql://localhost:5432/nirvaha_db"));
        assertTrue(DatabaseIsolationTestSafeguard.isForbiddenDevDatabase(null, "jdbc:postgresql://localhost:5432/nirvaha_db?ssl=false"));

        // Permitted test database
        assertFalse(DatabaseIsolationTestSafeguard.isForbiddenDevDatabase("nirvaha_test_db", "jdbc:postgresql://localhost:5432/nirvaha_test_db"));
        assertFalse(DatabaseIsolationTestSafeguard.isForbiddenDevDatabase("nirvaha_test_db", "jdbc:postgresql://localhost:5432/nirvaha_test_db?ssl=false"));
        assertFalse(DatabaseIsolationTestSafeguard.isForbiddenDevDatabase("h2", "jdbc:h2:mem:testdb"));
    }
}
