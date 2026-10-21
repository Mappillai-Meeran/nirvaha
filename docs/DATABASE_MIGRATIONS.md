# Nirvaha — Database Provisioning & Migration Architecture

This document defines the database initialization, schema provisioning, baseline strategy, and migration lifecycle for the Nirvaha Civic Intelligence Platform.

---

## 1. Executive Summary

Nirvaha uses **Flyway** (`org.flywaydb:flyway-core` + `flyway-database-postgresql` with `spring-boot-starter-flyway` v12.4.0 / Spring Boot 4.1.1) to provide automated, reproducible database initialization and migration on **PostgreSQL 16+**.

- **Clean Fresh Deployments**: A brand-new empty database boots cleanly in production without Hibernate schema auto-generation (`ddl-auto=validate`). Flyway executes `V1__initial_schema.sql` to establish the schema.
- **Existing Environments**: Development databases without migration history automatically baseline at Version 1 (`baseline-on-migrate=true`, `baseline-version=1`), preserving existing tables and seed data without conflicting DDL operations.
- **Safety Standard**: Hibernate schema auto-generation (`update` or `create-drop`) is eliminated in production; Hibernate runs in `validate` mode to ensure strict schema fidelity against JPA entity mappings.

---

## 2. Migration Directory Structure

All active Flyway migration scripts reside on the classpath at:
```
nirvaha-backend/src/main/resources/db/migration/
└── V1__initial_schema.sql
```

Legacy incremental migration files (historical deltas created prior to the initial repository baseline) are archived under:
```
docs/archive/legacy_migrations/
├── V1__demo_run_ownership.sql
├── V2__password_reset_tokens.sql
├── V3__resident_feedback_and_disputes.sql
├── V4__resident_feedback_resolution_unique_index.sql
├── V5__service_rules_official_sla.sql
└── V6__sla_escalation_tracking.sql
```
*Note: The `docs/` tree is outside the Java application classpath, ensuring legacy scripts cannot accidentally execute or conflict with active migrations.*

---

## 3. Schema Topology & Table Dependency Matrix

The initial baseline `V1__initial_schema.sql` provisions **19 tables**, **272 columns**, **19 identity sequences**, **47 indexes**, and **79 constraints** in strict topological dependency order:

| Level | Table Name | Key Dependencies | Primary Purpose |
|:---|:---|:---|:---|
| **0** | `users` | None | Citizen, Officer, Admin authentication & roles |
| **0** | `wards` | None | Municipal ward administrative centroids |
| **0** | `data_sources` | None | Registry of GIS, weather, and policy data feeds |
| **0** | `road_segments` | None | OSM road network geometries and bounding boxes |
| **1** | `administrative_areas` | `data_sources`, Self | Multi-tier administrative boundaries (UT to Village) |
| **1** | `service_rules` | `data_sources` | Statutory citizen charter SLAs and warning thresholds |
| **1** | `weather_observations` | `data_sources` | IMD & Open-Meteo telemetry observations |
| **1** | `password_reset_tokens` | `users` | SHA-256 hashed single-use password recovery |
| **1** | `risk_assessments` | `wards` | Spatial and temporal multi-factor risk scores |
| **2** | `incidents` | `users`, `administrative_areas`, `data_sources` | Core civic issue reports and intake metadata |
| **2** | `intelligence_signals` | `administrative_areas` | Emerging cluster, hotspot, and trend signals |
| **3** | `evidence` | `incidents` | Photo/video evidence with SHA-256 integrity hashes |
| **3** | `incident_actions` | `incidents`, `users` | Immutable audit log of officer lifecycle actions |
| **3** | `incident_service_tracking` | `incidents`, `service_rules` | SLA clocks, escalation levels, and deadline alerts |
| **3** | `notifications` | `users`, `incidents` | Multi-channel dispatch with deduplication keys |
| **3** | `officer_decisions` | `incidents`, `users` | Human officer recommendations and AI overrides |
| **3** | `anomaly_flags` | `incidents` | Evidence reuse and bulk-closure integrity alerts |
| **4** | `resolutions` | `incidents`, `users`, `evidence` | Resolution records with SSIM verification scores |
| **5** | `incident_feedbacks` | `incidents`, `users`, `resolutions` | Resident confirmation and dispute adjudication |

---

## 4. Environment Profiles & Baseline Behavior

### Profile Configuration
```properties
# Base application.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=${FLYWAY_BASELINE_ON_MIGRATE:true}
spring.flyway.baseline-version=1
spring.flyway.validate-on-migrate=true

# Development application-dev.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.flyway.validate-on-migrate=true
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# Production application-prod.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=${FLYWAY_BASELINE_ON_MIGRATE:false}
spring.flyway.baseline-version=1
spring.flyway.validate-on-migrate=true
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
```

### Exact Flyway Behavior Under Different Database States

| Database State | `baseline-on-migrate` Setting | Flyway Action | `V1__initial_schema.sql` Execution | Recorded State in `flyway_schema_history` |
| :--- | :--- | :--- | :--- | :--- |
| **Clean / Empty Database** | `true` or `false` | Baseline is a NO-OP; Flyway detects empty database and runs migrations from version 1 | **EXECUTED** | `installed_rank: 1`, `type: SQL`, `state: SUCCESS` |
| **Existing Database with Schema (No Flyway History)** | `true` | Flyway inserts baseline entry at version 1; does not run DDL | **SKIPPED** (Preserved) | `installed_rank: 1`, `type: BASELINE`, `state: BASELINE` |
| **Existing Database with Schema (No Flyway History)** | `false` | Flyway throws `FlywayException` (schema non-empty without history) | **NOT EXECUTED** (Fails fast) | None |
| **Damaged / Unrelated Non-Empty Database** | `true` | Flyway baselines at version 1; Hibernate `ddl-auto=validate` then fails immediately | **SKIPPED** | `type: BASELINE`, followed by Hibernate validation crash |

### Production Safety Recommendation
- For **fresh production deployments** (new cloud databases), Flyway automatically treats empty databases as unmigrated and runs `V1__initial_schema.sql`.
- For **pre-existing production databases** being migrated to Flyway for the first time, operators can set `FLYWAY_BASELINE_ON_MIGRATE=true` during the initial deployment, or run an explicit `flyway baseline` command to prevent unintended baselining of damaged or misconfigured databases.

---

## 5. Deployment & Execution Scenarios

### Scenario A: Fresh Production Database Deployment
1. A fresh PostgreSQL instance or database is created (empty, no tables).
2. The application starts with `SPRING_PROFILES_ACTIVE=prod`.
3. Spring Boot Flyway auto-configuration executes `V1__initial_schema.sql`.
4. Flyway creates `flyway_schema_history` and records version `1` as `SUCCESS`.
5. Hibernate initializes with `ddl-auto=validate`, verifies that all 19 entities match the tables/columns/constraints exactly, and the application starts successfully.

### Scenario B: Existing Development Database
1. A developer connects to an existing local database (`nirvaha_db`) with pre-existing tables.
2. Flyway detects existing tables and no `flyway_schema_history`.
3. Because `baseline-on-migrate=true` and `baseline-version=1`, Flyway creates `flyway_schema_history` with a `BASELINE` record at Version 1.
4. `V1__initial_schema.sql` is skipped; existing tables and data remain untouched.
5. Hibernate validates the schema and the backend starts immediately.

---

## 6. How to Add Future Schema Changes

When introducing schema modifications in future features:

1. **Naming Standard**: Place new scripts under `src/main/resources/db/migration/` using standard Flyway version formatting:
   `V2__add_new_feature_table.sql`
   `V3__update_incident_indexes.sql`

2. **Idempotency & Zero-Downtime**:
   - Use `ADD COLUMN IF NOT EXISTS` where supported.
   - For nullable columns or columns with defaults, avoid blocking exclusive locks.
   - For unique constraints, create unique partial or concurrent indexes where appropriate.

3. **Entity Synchronization**:
   - Update the corresponding JPA entity class annotations to match the new columns or tables.
   - Ensure `@Enumerated(EnumType.STRING)` check constraints match Java enum constants.

---

## 7. Automated Test Verification

Database initialization and schema validation are verified by automated tests:
- **`ProductionConfigurationTest.java`**: Verifies that `spring.flyway.enabled=true`, `spring.jpa.hibernate.ddl-auto=validate`, and `V1__initial_schema.sql` exists on classpath.
- **`DatabaseMigrationLifecycleTest.java`**:
  - Tests Flyway bean configuration and active migration state.
  - Verifies second migration execution idempotency (0 migrations executed, no duplicate runs).
  - Verifies all 19 platform tables exist in the database catalog.
  - Verifies `flyway.validate()` executes without validation exceptions.
- **`NirvahaBackendApplicationTests.java`**: Verified against a clean empty test database (`nirvaha_migration_test`), confirming full migration execution and Hibernate schema validation passing with exit code 0.
- **Backend Test Suite**: **373 tests run, 0 failures, 0 errors, 0 skipped**.
