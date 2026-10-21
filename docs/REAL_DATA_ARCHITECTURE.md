# REAL DATA ARCHITECTURE — NIRVAHA PHASE 12

## Overview & Architecture Principles

Nirvaha Phase 12 establishes a production-oriented foundation for consuming, validating, connecting, and explaining **real civic data with explicit data provenance**.

```
COLLECT → VALIDATE → NORMALIZE → ATTRIBUTE SOURCE → TIMESTAMP → GEOSPATIAL CONNECTION → STORE → DERIVE INTELLIGENCE → EXPLAIN
```

Every data object in Nirvaha is explicitly categorized into one of four source classifications:

1. **OFFICIAL**: Government-published source (e.g. Puducherry LAD delimitation maps, IMD rainfall feeds, PWD Citizen Charter).
2. **CITIZEN**: Actual reports and evidence submitted by citizens through Nirvaha.
3. **DERIVED**: Analytical calculations computed by Nirvaha (e.g. Risk score, Priority score, SLA clock calculation).
4. **SIMULATED**: Artificial data created for stress-testing or development.

---

## Core Components

### 1. Data Source Registry (`data_sources`)
Persists all registered external sources with provider name, dataset name, retrieval method, freshness status (`CURRENT`, `STALE`, `OUTDATED`, `UNKNOWN`), update frequency, license, and SHA-256 payload hashes.

### 2. Administrative Area Foundation (`administrative_areas`)
Persists verified ward boundary geometries with explicit boundary metadata (`geometry_status = DERIVED_FROM_OFFICIAL`), bounding box indexing, and validation status (`VERIFIED`, `REVIEW_REQUIRED`, `INVALID`).

### 3. Geospatial Point-In-Polygon Engine
Executes ray-casting point-in-polygon containment checks for incident coordinates against verified administrative ward polygons.
- Points contained in 1 verified polygon: `assignment_method = POINT_IN_POLYGON`, `assignment_confidence = 1.0`.
- Points outside verified polygons: `assignment_method = UNASSIGNED`, `ward_assignment_notes = "WARD ASSIGNMENT REVIEW REQUIRED"`.
- Points matching multiple overlapping polygons: `assignment_method = UNASSIGNED`, `ward_assignment_notes = "WARD ASSIGNMENT REVIEW REQUIRED"`.

### 4. IMD Meteorological Adapter (`weather_observations`)
Ingests daily and historical IMD rainfall observations for Puducherry District. Provides deduplication by logical key `(provider, station_code, observation_type, observed_at)` and raw SHA-256 payload hash. Generates contextual rainfall explanations for drainage/flooding incidents.

### 5. Policy SLA Tracking (`service_rules` & `incident_service_tracking`)
Tracks Citizen Charter SLAs against published policy references (e.g. PWD 15-day urban drainage maintenance target). Calculates elapsed days and SLA status (`WITHIN_STANDARD`, `APPROACHING_DEADLINE`, `OVERDUE`, `COMPLETED`, `NOT_APPLICABLE`).

### 6. Simulated Data Isolation
Stress-test and simulated records carry `classification_tag = SIMULATED` and are strictly excluded from production ward risk intelligence calculations.
