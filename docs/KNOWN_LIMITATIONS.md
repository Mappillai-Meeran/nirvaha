# NIRVAHA — KNOWN SYSTEM LIMITATIONS

To maintain maximum technical integrity and competition transparency, the following technical and operational boundaries of the current Nirvaha implementation are explicitly documented.

---

## 1. Environment & Infrastructure Boundaries

- **Docker Runtime Execution**: Dockerfiles (`Dockerfile`, `docker-compose.yml`) are fully constructed and verified as valid build artifacts. However, live runtime execution in the evaluation environment was blocked by local Windows/WSL2 distribution unavailability. The platform executes synchronously via standalone Spring Boot runtime (`mvnw.cmd test` / `mvnw.cmd spring-boot:run`).
- **External Paid Translation APIs**: To guarantee zero runtime downtime, Nirvaha intentionally avoids paid external translation API dependencies. Language detection and normalization use an offline Local Rule Provider (`LOCAL_RULES`, `nirvaha-language-v1`) based on Unicode script range analysis and deterministic keyword dictionaries across EN, TA, HI, ML, and TE.

---

## 2. Geospatial & Boundary Coverage

- **Digitized Gazette Wards**: Real point-in-polygon ward matching is currently configured for three digitized Puducherry municipal wards:
  - Oulgaret Ward 12
  - Puducherry Heritage Boulevard Ward 5
  - Muthialpet North Ward 8
- **Outside Boundary Behavior**: Incidents reported outside these digitized polygons are flagged as `UNASSIGNED / WARD ASSIGNMENT REVIEW REQUIRED` to prevent false spatial assignment.

---

## 3. Data & Provenance Boundaries

- **Government Ecosystem Classification**: Dataset sources (Puducherry Gazette boundaries, IMD weather observations, PWD SLA rules) are classified as `DERIVED_FROM_OFFICIAL` or `REFERENCE`. Nirvaha does NOT claim live production API integrations or formal government partnerships where only public data sources and references are configured.
- **Simulated Test Data Isolation**: Demonstration stress-test records are tagged as `SIMULATED` and strictly filtered out from production risk forecasting, spatial hotspots, and learning signals.

---

## 4. Operational Boundaries

- **Decision Support Framing**: Nirvaha's action recommendation engine operates strictly as municipal decision support. It does not perform autonomous government actions or override officer decisions. Officers retain full control via `ACCEPT`, `MODIFY`, `DEFER`, and `REJECT` decision recording.
- **Integrity Radar Framing**: Anomaly detection flags patterns (`ANOMALY DETECTED — REVIEW RECOMMENDED`) without asserting corruption or fraud accusations against citizens or officers.
