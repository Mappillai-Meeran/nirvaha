# Nirvaha Phase 5C.5 — Curated OSM Road Intelligence & Historical Rainfall Visualization Report

**Document Date:** October 21, 2026  
**Execution Environment:** `E:\Nirvaha`  
**System Evaluated:** Nirvaha Civic Intelligence Platform (Spring Boot 4.1.1 Backend + React/Vite Frontend)  
**Final Status:** **PASS**

---

## Executive Summary

Phase 5C.5 delivers two tightly controlled civic intelligence capabilities to the Nirvaha platform without destabilizing existing administrative boundaries, duplicate detection thresholds, or statutory SLA evaluation pipelines:

1. **Curated OpenStreetMap (OSM) Road Reference Layer:**
   - Ingested 221 genuine OpenStreetMap arterial and collector road features covering the Puducherry urban agglomeration (`[11.90°N, 79.79°E]` to `[11.97°N, 79.85°E]`) under the Open Database License (ODbL) 1.0.
   - Built a robust, offline-capable `RoadReferenceService` featuring geometry validation, bounding box spatial filtering, and sub-meter point-to-polyline distance projection.
   - Isolated road proximity as **strictly supplementary context** on incident readouts (`IncidentResponse`), ensuring zero automated changes to assigned wards, priority scores, ownership, or SLA clocks.

2. **Historical Precipitation Visualization in Government Command Center:**
   - Connected the existing `/api/provenance/weather/history` endpoint to a dedicated, responsive `HistoricalPrecipitationPanel` in the Government Command Center.
   - Grounded the visualization in the Open-Meteo ERA5 / ERA5-Land reanalysis archive (CC BY 4.0), equipped with honest loading, cached, empty, and unavailable error states.
   - Enforced strict provenance labeling: explicitly identified as **historical atmospheric reanalysis**, not live rain gauges, flood sensors, or confirmed street waterlogging.

---

## 1. Preflight Audit Findings & Baseline Results

Before making code modifications, a comprehensive inspection was conducted across backend entities, repositories, controllers, services, and frontend routes:

| Component / Subsystem | Preflight Status | Audit Finding |
| :--- | :--- | :--- |
| **Historical Weather Endpoint** | Existing & Verified | `ProvenanceController.getHistoricalPrecipitation()` was implemented and routing to `WeatherService.fetchHistoricalPrecipitation()`. Verified query parameters (`latitude`, `longitude`, `startDate`, `endDate`), in-memory caching with 6-hour TTL, and fallback handling. |
| **Road Reference Entity/Model** | Absent | No road entity or table existed in `nirvaha-backend`. Existing GIS assets consisted exclusively of `administrative_areas` (polygons/centroids for the 42 wards). Reusing ward envelopes for roads would have corrupted administrative semantics; thus, a dedicated `RoadSegment` entity was required. |
| **Geospatial Math Utilities** | Partial | `GeospatialUtils.java` had ray-casting point-in-polygon math, but lacked polyline coordinate parsing, point-to-segment perpendicular distance calculation, and polyline length computation. |
| **Duplicate Detection** | Untouched | `DuplicateDetectionService` relies on text embedding similarity and 500m Haversine distance; road proximity matching was kept strictly decoupled from duplicate scoring. |
| **SLA Tracking Engine** | Untouched | `SlaEvaluationScheduler` and `IncidentServiceTracking` operate on statutory rules and elapsed days. Kept strictly independent of external weather or road references. |

---

## 2. Source URLs, Data Artifacts & Licensing

To eliminate network vulnerability and prevent runtime rate limits from public servers, a reproducible, bounded GeoJSON extract was captured for Puducherry and packaged as an immutable application resource.

### OpenStreetMap (OSM) Road Reference Layer
- **Provider:** OpenStreetMap Foundation (OSMF) / Overpass API
- **Extract Query:** Bounded arterial road extraction covering Puducherry urban center:
  ```overpass
  [out:json][timeout:25];
  (
    way["highway"~"motorway|trunk|primary|secondary|tertiary|unclassified|residential"]
      (11.90,79.79,11.97,79.85);
  );
  out body;
  >;
  out skel qt;
  ```
- **Local Artifact Location:** `E:\Nirvaha\nirvaha-backend\src\main\resources\data\osm_puducherry_arterial_roads.geojson`
- **Features Extracted:** 221 valid GeoJSON `LineString` features.
- **Coverage:** Bounding box `[minLat: 11.9000, minLng: 79.7900, maxLat: 11.9700, maxLng: 79.8500]`.
- **License / Attribution:** Open Database License (ODbL) 1.0 / © OpenStreetMap contributors.
- **Runtime Policy:** **Zero runtime Overpass queries.** All operational matches are executed against the validated local database table populated from the curated classpath artifact.

### Historical Precipitation Dataset
- **Provider:** Open-Meteo Historical Weather Archive (`archive-api.open-meteo.com`)
- **Underlying Reanalysis Models:** ECMWF ERA5 & ERA5-Land (0.25° / ~25km spatial atmospheric assimilation grid)
- **License:** Creative Commons Attribution 4.0 International (CC BY 4.0)
- **Parameters:** Default coordinates `11.9156° N, 79.8296° E` (Puducherry Municipal Center); daily/hourly aggregation in millimeters (`mm`).
- **Cache Strategy:** In-memory ConcurrentHashMap cache keyed by `lat:lon:startDate:endDate` with a 6-hour expiry window.

---

## 3. Files Changed and Architectural Decisions

```
E:\Nirvaha\
├── nirvaha-backend\
│   ├── src\main\resources\data\
│   │   └── osm_puducherry_arterial_roads.geojson            [Created: 221 genuine OSM LineStrings]
│   ├── src\main\java\com\nirvaha\nirvaha_backend\
│   │   ├── entity\RoadSegment.java                         [Created: JPA entity for curated roads]
│   │   ├── repository\RoadSegmentRepository.java           [Created: Spatial bounding box queries]
│   │   ├── dto\
│   │   │   ├── RoadMatchContext.java                       [Created: Match distance, name, confidence DTO]
│   │   │   ├── RoadImportSummary.java                      [Created: Idempotent ingestion summary DTO]
│   │   │   └── IncidentResponse.java                       [Updated: Added supplementary road match fields]
│   │   ├── util\GeospatialUtils.java                       [Updated: Polyline parsing, distance & length math]
│   │   ├── service\
│   │   │   ├── RoadReferenceService.java                   [Created: Idempotent ingest & spatial road snap]
│   │   │   └── IncidentService.java                        [Updated: Supplementary road context enrichment]
│   │   └── controller\ProvenanceController.java            [Updated: /roads/summary and /roads/nearest endpoints]
│   └── src\test\java\com\nirvaha\nirvaha_backend\
│       ├── RoadReferenceServiceTest.java                   [Created: 6 comprehensive unit/integration tests]
│       └── ProvenanceControllerTest.java                   [Updated: Added road summary & nearest tests]
└── nirvaha-frontend\
    ├── src\
    │   ├── services\index.js                               [Updated: Added getHistoricalWeather, getRoadSummary, getNearestRoad]
    │   ├── components\government\
    │   │   ├── HistoricalPrecipitationPanel.jsx            [Created: Historical weather UI with ERA5 disclaimers]
    │   │   └── CommandCenterSidebar.jsx                    [Updated: Added Historical Rainfall nav button]
    │   └── pages\government\
    │       ├── CommandCenter.jsx                           [Updated: Mounted Historical Rainfall view & road chip]
    │       └── IncidentDetail.jsx                          [Updated: Added OSM Road Reference in geospatial ledger]
```

### Architectural Decisions

1. **Decoupled Road Reference vs. Administrative Boundaries:**
   - Ward administrative boundaries (`administrative_areas`) represent official municipal jurisdiction boundaries for dispatch.
   - Road segments (`road_segments`) represent supplementary physical infrastructure corridors from OSM.
   - Roads are never allowed to override ward polygons or alter assigned ward IDs.
2. **Deterministic Idempotency:**
   - Roads are indexed by `osm_feature_id`. Consecutive imports update existing records rather than producing duplicates.
   - Validation ensures coordinate bounds conform to the Puducherry metropolitan zone and geometry types are strictly `LineString`.
3. **Planar Geospatial Distance Projection:**
   - Implemented metric segment distance in `GeospatialUtils` utilizing local latitude scaling factor `cos(avgLat)`:
     $$\Delta x = (\lambda_2 - \lambda_1) \times 111412.84 \times \cos(\phi_{\text{avg}})$$
     $$\Delta y = (\phi_2 - \phi_1) \times 111132.954$$
   - Provides sub-meter accuracy within Puducherry (~11.93°N) without introducing bulky external GIS native libraries (e.g. GDAL/PostGIS extensions).
4. **Non-Mutating Incident Enrichment:**
   - Road snapping is applied during DTO transformation in `IncidentService.enrichIncidentResponse()`. The underlying `Incident` database entity is not modified.

---

## 4. Import Results & Ingestion Statistics

The idempotent ingestion pipeline in `RoadReferenceService` was validated against both test scenarios and the production dataset:

```
[INFO] c.n.n.service.RoadReferenceService: Starting idempotent OSM Road Reference ingestion from 'data/osm_puducherry_arterial_roads.geojson'
[INFO] c.n.n.service.RoadReferenceService: OSM Road Import completed: 221 read, 221 accepted, 0 duplicates updated, 0 rejected. Total road length: 168.42 km
```

### Ingestion Metric Breakdown

| Metric | Production Dataset | Test Suite (Malformed/Duplicate Runs) |
| :--- | :--- | :--- |
| **Total Features Parsed** | 221 | 8 |
| **Accepted & Verified Records** | 221 | 4 |
| **Updated Existing Records (Idempotent)** | 0 (Initial) / 221 (Repeat) | 2 |
| **Rejected Records** | 0 | 2 |
| **Rejection Reasons** | N/A | 1x Non-LineString (`Point`), 1x Out-of-Bounds (`85.0° N`) |
| **Total Bounded Road Network Length**| **168.42 km** | 0.33 km |
| **Coverage Area** | Puducherry Urban Agglomeration | Test Fixtures |
| **Primary Highway Classes Ingested** | `primary`, `secondary`, `tertiary`, `trunk`, `residential` | `primary`, `residential` |

---

## 5. API Contracts & UI Behavior

### A. Road Reference Endpoints

#### `GET /api/provenance/roads/summary`
- **Description:** Returns metadata on the active road reference layer.
- **Access:** Public / Municipal Officer (Read-Only)
- **Response Schema:**
  ```json
  {
    "totalSegments": 221,
    "totalLengthKm": 168.42,
    "lastImportedAt": "2026-10-21T10:48:42",
    "extractVersion": "2026-10-OSM-PUDUCHERRY-V1",
    "source": "OpenStreetMap Contributors",
    "license": "Open Database License (ODbL) 1.0",
    "coverageBoundingBox": "[11.90, 79.79, 11.97, 79.85]",
    "provenanceClass": "OFFICIAL_EXTERNAL_DATA"
  }
  ```

#### `GET /api/provenance/roads/nearest?latitude={lat}&longitude={lon}&maxRadiusMeters={radius}`
- **Description:** Finds the closest arterial road segment within the specified search radius (default 300m).
- **Response Schema (Match Found):**
  ```json
  {
    "matched": true,
    "osmFeatureId": "way/24859012",
    "roadName": "Subbiah Salai",
    "roadReference": "RC-4",
    "highwayType": "primary",
    "distanceMeters": 42.6,
    "confidence": "HIGH",
    "source": "OpenStreetMap",
    "license": "ODbL 1.0",
    "explanation": "Matched to primary road 'Subbiah Salai' (43m proximity). Supplementary spatial context only; not official government infrastructure."
  }
  ```
- **Response Schema (No Match within Radius):**
  ```json
  {
    "matched": false,
    "confidence": "NONE",
    "explanation": "No arterial road match within 300m search radius."
  }
  ```

---

### B. Historical Precipitation Endpoint & UI Panel

#### `GET /api/provenance/weather/history?latitude={lat}&longitude={lon}&startDate={YYYY-MM-DD}&endDate={YYYY-MM-DD}`
- **Response Schema:**
  ```json
  {
    "latitude": 11.9156,
    "longitude": 79.8296,
    "startDate": "2020-11-24",
    "endDate": "2020-11-27",
    "totalRainfallMm": 184.2,
    "maxHourlyRainfallMm": 28.5,
    "available": true,
    "provider": "Open-Meteo Historical Weather Archive",
    "license": "Creative Commons Attribution 4.0 International (CC BY 4.0)",
    "retrievedAt": "2026-10-21T10:45:00",
    "provenanceClass": "OFFICIAL_EXTERNAL_DATA",
    "isCached": false,
    "hourlyData": [
      { "time": "2020-11-24T00:00", "precipitationMm": 0.0 },
      { "time": "2020-11-25T14:00", "precipitationMm": 28.5 }
    ]
  }
  ```

#### UI Panel Features (`HistoricalPrecipitationPanel.jsx`)
- **Navigation:** Accessible directly via the **Historical Rainfall** button (`BarChart3` icon) under the **Intelligence & Analysis** sidebar group in the Government Command Center.
- **Date Range Controls:** Interactive date pickers with validation restricting queries to past dates and a maximum window of 31 days.
- **Historical Benchmark Presets:** Quick-select buttons for **Past 7 Days**, **Past 14 Days**, **Cyclone Nivar (Nov 2020)**, and **Northeast Monsoon Peak (Nov 2021)**.
- **Visualization:** High-resolution SVG time-series histogram displaying hourly rainfall intensity (mm/h) color-coded by IMD standards:
  - *Blue:* Light (< 2.5 mm/h)
  - *Amber:* Moderate (2.5 – 7.5 mm/h)
  - *Red:* Heavy (> 7.5 mm/h)
- **Direct Metrics:** Displays backend-calculated cumulative rainfall (mm) and peak hourly intensity without duplicating calculations in the browser.
- **Audit Integrity Disclaimer:** Prominently displays the required civic integrity notice:
  > *"This dataset represents atmospheric reanalysis modeling, not an in-situ street rain gauge or telemetry from stormwater culverts. It does not confirm live street inundation, active waterlogging, or alter statutory grievance SLAs."*

---

## 6. Security and Simulated-Data Isolation Checks

| Safety Check | Requirement | Result | Evidence |
| :--- | :--- | :--- | :--- |
| **Authentication & Role Boundaries** | Preserve existing security rules | **PASS** | `/api/provenance/**` routes are registered under public read-only access in `SecurityConfig.java`. Endpoints expose only aggregated weather data and public road geometries; zero user profiles, resident contact info, or confidential grievance details are reachable. |
| **No Civic Mutation** | Road matches must never mutate incident entities | **PASS** | `RoadMatchContext` is attached solely to `IncidentResponse` DTOs. Database entities (`Incident`, `AdministrativeArea`, `IncidentServiceTracking`) remain completely unchanged. |
| **Simulated Data Decoupling** | Simulated data must not leak into official statistics | **PASS** | `RoadReferenceService` operates exclusively on real OSM geometries. Historical weather reanalysis is completely segregated from live weather and the Monsoon Stress Simulator (`MonsoonStressSimulator.jsx`). |
| **Database Schema Safety** | No runtime DDL or unreviewed migrations | **PASS** | Hibernate schema validation `validate` is enforced. Table `road_segments` is mapped cleanly via standard JPA annotations. |
| **Privacy & Leakage** | No stack traces or raw upstream errors exposed | **PASS** | Upstream Open-Meteo connection timeouts and 5xx errors return structured fallback JSON payloads with clear error categories (`UPSTREAM_UNAVAILABLE`, `RATE_LIMITED`). |

---

## 7. Tests Executed & Actual Results

### Backend JUnit Test Suite

Executed via `./mvnw test`:

```
[INFO] Running com.nirvaha.nirvaha_backend.RoadReferenceServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.136 s
[INFO] Running com.nirvaha.nirvaha_backend.ProvenanceControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.812 s
[INFO] Running com.nirvaha.nirvaha_backend.GeospatialAssignmentTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 27.43 s
[INFO] Running com.nirvaha.nirvaha_backend.IncidentServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.887 s
[INFO] Running com.nirvaha.nirvaha_backend.WeatherServiceTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.644 s
[INFO] Running com.nirvaha.nirvaha_backend.DataReliabilityIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 8.520 s
[INFO] Running com.nirvaha.nirvaha_backend.DuplicateDetectionServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.271 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

#### Detailed Test Verification Coverage:
1. **Valid LineString Ingestion:** Verified that valid WGS84 GeoJSON features are stored with accurate bounding boxes and lengths.
2. **Malformed & Empty Geometry Rejection:** Confirmed rejection of non-LineString features (e.g. `Point`) and coordinates outside Puducherry bounding box (`85.0° N`).
3. **Idempotency & Duplicate Handling:** Repeated import of duplicate `osm_feature_id` updates existing record attributes without generating duplicate rows.
4. **Missing Artifact Handling:** Service gracefully handles absent classpath artifacts by logging a warning without throwing startup fatal exceptions.
5. **Spatial Distance Snapping:** Tested point snapping to polyline; verified accurate distance calculation and confidence rating (`HIGH` < 50m, `MEDIUM` 50-150m, `LOW` 150-300m).
6. **Search Radius Thresholds:** Confirmed points beyond the search radius return `matched: false`.
7. **Weather Archive Timeouts & 5xx Handling:** Verified mock server simulations for Open-Meteo HTTP 503, 429 rate limit, 3-second connection timeouts, and malformed HTML payloads.
8. **Provenance Integrity:** Verified cache hits (`isCached: true`) preserve upstream timestamps, licenses, and units.

---

### Frontend Static Analysis & Production Build

Executed via `npm run lint` and `npm run build`:

```
> nirvaha-frontend@0.0.0 lint
Found 0 errors.

> nirvaha-frontend@0.0.0 build
vite v8.2.2 building client environment for production...
✓ 1945 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                     0.46 kB │ gzip:   0.30 kB
dist/assets/index-BQrkvM8B.css     95.52 kB │ gzip:  22.69 kB
dist/assets/index-CrKMk1pE.js   1,008.44 kB │ gzip: 280.62 kB
✓ built in 15.13s
```

---

## 8. Known Limitations & Unimplemented Items

1. **OSM Pedestrian & Service Lanes Excluded:**
   - In accordance with phase constraints to avoid bloated in-memory spatial indexes, only major arterial, collector, and primary residential roads (221 segments, 168.42 km) are included. Footways, cycle paths, and private service lanes were intentionally omitted.
2. **Historical Weather Time-Lag:**
   - Open-Meteo's ERA5 historical archive operates on a reanalysis pipeline with a 2 to 5-day lag from real time. The date picker explicitly restricts the end date to `today - 1 day` and provides an honest explanation if an unassimilated recent date is requested.
3. **Planar Distance Approximation at High Latitudes:**
   - The metric conversion utilizes planar projection with $\cos(\text{avgLat})$ scaling. While accurate to within centimeters in tropical latitudes (Puducherry at 11.93°N), high-latitude deployments would require geodesic ellipsoidal math (Vincenty or PostGIS `ST_Distance`).

---

## 9. Manual Verification Steps for the Government Command Center

To inspect the newly integrated capabilities in a live browser session:

1. **Start the Nirvaha Application Stack:**
   - Backend: `./mvnw spring-boot:run` in `E:\Nirvaha\nirvaha-backend` (port 8080).
   - Frontend: `npm run dev` in `E:\Nirvaha\nirvaha-frontend` (port 5173).
2. **Access the Government Command Center:**
   - Navigate to `http://localhost:5173/command`.
   - Log in using municipal officer credentials (`officer@nirvaha.gov.in` / `officer123`).
3. **Inspect the Historical Rainfall Panel:**
   - On the left sidebar under **Análise / Intelligence**, click **Historical Rainfall** (`ERA5` pill).
   - Verify that the **Historical Precipitation Telemetry & Reanalysis** panel renders with the prominent orange **Integrity Notice** banner.
   - Click the **Cyclone Nivar (2020)** preset button. Observe the SVG bar chart rendering hourly peaks with red bars indicating severe intensity (> 7.5 mm/h).
   - Hover over individual bars to view precise hourly rainfall (e.g. `28.5 mm`) and timestamps.
   - Confirm the metadata footer states: `Provider: Open-Meteo Historical Weather Archive` and `License: CC BY 4.0`.
4. **Inspect Supplementary OSM Road Context:**
   - In the sidebar, click **All Open Incidents**.
   - Inspect the incident list items: notice the emerald `OSM Ref: [Road Name] ([Distance]m)` chip appearing below the address line.
   - Click on **Case File** for an incident (e.g. Case #1) to open `IncidentDetail.jsx`.
   - In the **Geospatial & Provenance Ledger** card, observe the **Nearest Road Reference** box detailing the road name, proximity in meters, confidence rating, and attribution to OpenStreetMap (ODbL 1.0).

---

## Final Phase Status

# **PASS**

All requirements of Nirvaha Phase 5C.5 have been implemented, tested, and verified without breaking existing civic workflows, duplicate detection models, administrative ward envelopes, or statutory SLA policies.
