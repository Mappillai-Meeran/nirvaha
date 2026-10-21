# VBYLD 2027 — DEMONSTRATION DATA SPECIFICATION

## Data Classification & Provenance Matrix

To ensure 100% transparency during competition evaluation, all data utilized in Nirvaha is classified into four distinct provenance tiers:

| Tier | Category | Description | Examples |
|---|---|---|---|
| **Tier 1** | `DERIVED_FROM_OFFICIAL` | Boundaries, SLA policies, & weather observations derived from official public government publications. | Digitized Puducherry Gazette ward polygons, PWD SLA rules, IMD AWS rainfall data. |
| **Tier 2** | `REFERENCE` | Configured open registry & mapping schemas. | OpenStreetMap GIS geometries, source registry metadata. |
| **Tier 3** | `CITIZEN` | Real-time citizen submitted reports and evidence. | Citizen complaints, uploaded JPEG photos, GPS coordinates. |
| **Tier 4** | `SIMULATED` | Synthetic stress-test & demonstration dataset records. | Seeded duplicate scenarios used for stress-testing. |

> **CRITICAL RULE**: `SIMULATED` records are strictly isolated and excluded from production risk forecasting, spatial hotspots, emerging issue detection, and learning signals.

---

## Deterministic Demo Dataset

### Scenario 1: Puducherry Monsoon Drainage Waterlogging (Primary Demo)

```text
Incident ID:           #101
Classification:        CITIZEN
Reporter Role:         CITIZEN (User ID: 2, "Anand Kumar")
Submission Language:   TA (Tamil)
Original Description:  "கனமழைக்குப் பிறகு இந்த வீதியில் சாக்கடை நீர் தேங்கி நிற்கிறது"
Normalized Description: "[Normalized TA Representation]: Report regarding DRAINAGE issue - \"கனமழைக்குப் பிறகு...\""
Canonical Category:    DRAINAGE
Severity:              HIGH
Location:              Mission Street / Heritage Boulevard, Puducherry
Latitude / Longitude:  11.9350 / 79.8300
Assigned Ward:         Puducherry Heritage Boulevard Ward 5 (Ward ID: 2)
Ward Assignment Notes: Point-In-Polygon match verified against Puducherry Gazette boundaries
SLA Policy:            PWD Drainage SLA — 24 Hours Target Resolution
Weather Context:       IMD AWS Station #43333 — 48mm Heavy Rainfall Observation Attached
Priority Score:        67.5 / 100 (HIGH Priority)
Corroboration:         Linked with Incident #102 (English) and #103 (Hindi) within 500m
```

### Scenario 2: Corroborating Multilingual Reports (500m Haversine Radius)

```text
Incident ID:           #102
Classification:        CITIZEN
Submission Language:   EN (English)
Description:           "Heavy waterlogging blocking Mission Street intersection after rain"
Canonical Category:    DRAINAGE
Location:              Mission Street North, Puducherry (11.9353 / 79.8302)

Incident ID:           #103
Classification:        CITIZEN
Submission Language:   HI (Hindi)
Description:           "बारिश के बाद सड़क पर भारी पानी भर गया है"
Canonical Category:    DRAINAGE
Location:              Heritage Boulevard Corner (11.9348 / 79.8298)
```

---

## Seeded Data Isolation Guarantee

All demonstration data scripts in `nirvaha-backend/src/main/resources/data.sql` and unit test fixtures use explicit classification tags. Intelligence services (`CivicIntelligenceOrchestrator`, `RiskIntelligenceService`, `ActionRecommendationService`) execute strict DB query filtering (`WHERE classification_tag != 'SIMULATED'`) to preserve 100% data trust.
