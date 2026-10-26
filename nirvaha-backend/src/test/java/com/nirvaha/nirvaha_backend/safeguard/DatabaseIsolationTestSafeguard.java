package com.nirvaha.nirvaha_backend.safeguard;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseIsolationTestSafeguard {

    private static final Logger log = LoggerFactory.getLogger(DatabaseIsolationTestSafeguard.class);
    public static final String FORBIDDEN_DEV_DB = "nirvaha_db";

    @Autowired(required = false)
    private DataSource dataSource;

    @PostConstruct
    public void verifyDatabaseIsolation() {
        if (dataSource != null) {
            assertSafeTestDatasource(dataSource);
        }
    }

    public static void assertSafeTestDatasource(DataSource ds) {
        try (Connection connection = ds.getConnection()) {
            String catalog = connection.getCatalog();
            String url = connection.getMetaData().getURL();
            log.info("Test Safeguard verified datasource: catalog='{}', url='{}'", catalog, url);

            if (isForbiddenDevDatabase(catalog, url)) {
                String errorMsg = String.format(
                        "FAIL-FAST TEST SAFEGUARD ABORT: Tests are connected to the development database '%s' (Catalog: %s, URL: %s). "
                                + "Destructive operations on '%s' are strictly forbidden. "
                                + "Please configure spring.datasource.url to point to an isolated test database (e.g. 'nirvaha_test_db').",
                        FORBIDDEN_DEV_DB, catalog, url, FORBIDDEN_DEV_DB);
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to inspect datasource for safety check: " + e.getMessage(), e);
        }
    }

    public static boolean isForbiddenDevDatabase(String catalog, String url) {
        if (catalog != null && catalog.equalsIgnoreCase(FORBIDDEN_DEV_DB)) {
            return true;
        }
        if (url != null) {
            String lowerUrl = url.toLowerCase();
            // Matches /nirvaha_db or /nirvaha_db?params but NOT /nirvaha_test_db or /nirvaha_db_test
            if (lowerUrl.matches(".*[/\\\\]nirvaha_db([?&#;].*)?$")) {
                return true;
            }
        }
        return false;
    }
}
