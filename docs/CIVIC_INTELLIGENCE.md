# NIRVAHA CIVIC INTELLIGENCE AT SCALE (PHASE 13)

## 1. Phase 13 Objective

Phase 13 transforms Nirvaha from an incident-recording platform into a pattern-level civic intelligence system. It identifies concentrations, trends, emerging problems, unresolved burdens, and spatial hotspots across time and geography while maintaining strict explainability, reproducibility, provenance, and simulated-data isolation.

Nirvaha intelligence outputs are decision-support analytical signals, NOT official government decisions or legal risk classifications.

---

## 2. Intelligence Architecture

The Phase 13 pipeline operates as a deterministic aggregation engine over eligible civic records:

```text
ELIGIBLE CIVIC DATA
       ↓
DATA QUALITY FILTER (Classification / State / Coordinates)
       ↓
TEMPORAL NORMALIZATION (24h, 7d, 30d, 90d & Previous Window Comparison)
       ↓
SPATIAL AGGREGATION & CATEGORY CLUSTERING (500m Geo Distance)
       ↓
TREND ANALYSIS (Deterministic threshold classification)
       ↓
EMERGING ISSUE DETECTION (Surge % + Minimum Incident Volume)
       ↓
RECURRING PROBLEM DETECTION (Multi-window repetition)
       ↓
UNRESOLVED LOAD & AGEING CALCULATIONS
       ↓
SYSTEM RESOLUTION PERFORMANCE METRICS
       ↓
CROSS-SIGNAL INTELLIGENCE (Rainfall Context + Risk Engine Integration)
       ↓
PERSISTENCE & MODEL VERSIONING (nirvaha-intelligence-v1)
       ↓
COMMAND CENTER & LEAFLET MAP VISUALIZATION
```

Phase 13 consumes outputs from Phase 5 (Priority Intelligence) and Phase 9 (Risk Intelligence) without altering their core formulas or weights.

---

## 3. Data Eligibility Rules

The `IntelligenceEligibilityService` provides a centralized policy for record participation:

* **Included Classifications**: `OFFICIAL`, `CITIZEN`, `DERIVED` records.
* **Excluded Classifications**: `SIMULATED` test fixtures are strictly excluded from production calculations.
* **Incident Validity**: Incidents must not be soft-deleted or marked as invalid/rejected duplicates.
* **Coordinate Validity**: Spatial calculations require valid latitude and longitude coordinates.
* **Timestamp Validity**: Timestamp must fall within the evaluated temporal window.

---

## 4. Temporal Windows

Supported temporal windows for analysis:

* `24_HOURS` (1 day lookback)
* `7_DAYS` (1 week lookback)
* `30_DAYS` (1 month lookback)
* `90_DAYS` (1 quarter lookback)

Previous period comparison calculates:
$$\text{Change \%} = \frac{\text{Current Count} - \text{Previous Count}}{\max(1, \text{Previous Count})} \times 100$$

Non-predictive terminology is enforced:
* **"Observed reporting increase"** instead of "Prediction".
* **"Recent activity increase"** instead of "Forecast".

---

## 5. Trend Analysis Algorithm

Trend analysis evaluates incident reporting velocity for a given ward and category over two consecutive time windows:

* **`INCREASING`**: Current count > Previous count AND $\text{Change \%} \ge +25.0\%$.
* **`DECREASING`**: Current count < Previous count AND $\text{Change \%} \le -25.0\%$.
* **`STABLE`**: Within $[-25\%, +25\%]$ change boundary.
* **`INSUFFICIENT_DATA`**: Total observations across both windows < 2 incidents.

All thresholds are deterministic and configurable.

---

## 6. Spatial Hotspot Detection Algorithm

Spatial hotspots detect geographical concentrations of incidents:

* **Grouping**: Group eligible incidents by category and administrative ward.
* **Clustering**: Compute spatial proximity using Haversine distance with a 500m radius threshold.
* **Centroid**: Compute spatial mean centroid ($\overline{\text{lat}}, \overline{\text{lng}}$).
* **Threshold**: Requires a minimum of 3 incidents within 500m during the evaluated time window.
* **Category Isolation**: Hotspots are category-specific (e.g. `DRAINAGE` vs `POTHOLE`).

---

## 7. Emerging Issue Detection

An emerging issue represents a surging municipal problem requiring proactive attention:

* **Criteria**: Current 7-day volume $\ge 3$ incidents AND $\text{Surge \%} \ge +50.0\%$ compared to previous 7 days.
* **Output**: Generates an `EMERGING_ISSUE` signal with evidence of surge velocity and unresolved burden.

---

## 8. Recurring Problem Detection

Identifies locations experiencing repeated civic issues across multiple temporal windows:

* **Evaluation**: Splits a 30-day lookback into three 10-day sub-periods.
* **Criteria**: Activity observed in at least 2 out of 3 distinct sub-periods for the same ward and category.
* **Output**: Generates a `RECURRING_PROBLEM` signal highlighting persistent infrastructural vulnerabilities.

---

## 9. Unresolved Load Calculation

Calculates municipal backlog and ageing metrics per ward and category:

* `totalReports`: Total incident volume in period.
* `unresolvedReports`: Incidents not in `RESOLVED` status.
* `overdueReports`: Incidents exceeding policy SLAs.
* `criticalHighPriorityCount`: Incidents with priority `CRITICAL` or `HIGH`.
* `averageAgeHours`: Average duration open.
* `oldestUnresolvedHours`: Age of oldest open incident.

---

## 10. System Resolution Performance Metrics

Evaluates system-wide resolution efficiency (without individual officer rankings or surveillance metrics):

* `averageAssignmentHours`: Time from creation to officer assignment.
* `averageStartHours`: Time from assignment to work start (`IN_PROGRESS`).
* `averageResolutionHours`: Time from work start to resolution.
* `verifiedResolutionPercentage`: Percentage of resolutions passing evidence verification.
* `reopenedPercentage`: Percentage of resolved incidents reopened by citizens.

---

## 11. Cross-Signal Intelligence & Weather Context

Combines multiple independent signals into explainable composite context:

* **IMD Weather Integration**: Correlates `DRAINAGE` or `WATER_SUPPLY` incident surges with recent IMD rainfall observations.
* **Language Rules**: Uses contextual wording (*"82.5 mm IMD rainfall context available"*) rather than causal claims (*"rainfall caused flooding"*).
* **Priority Alignment**: Connects pattern-level civic signals with Phase 5 priority distribution and Phase 9 risk scores.

---

## 12. Model Versioning

All Phase 13 intelligence signals are generated with explicit model versioning:

$$\text{modelVersion} = \text{"nirvaha-intelligence-v1"}$$

Formula or configuration adjustments must increment the model version tag to preserve auditability and historical reproducibility.

---

## 13. Structured Explainability Format

Every intelligence output produces structured, non-hallucinated explanations adhering to:

* **`WHAT`**: Signal type and category (e.g., "Emerging Issue: DRAINAGE").
* **`WHERE`**: Administrative ward and spatial centroid (e.g., "Muthialpet North Ward 8").
* **`WHEN`**: Evaluated temporal window (e.g., "Previous 7 days").
* **`WHY`**: Quantitative change metric (e.g., "14 reports vs 6 in previous period (+133.3%)").
* **`EVIDENCE`**: Unresolved count, priority breakdown, and weather context.
* **`SOURCE`**: Data classification and source provenance IDs.
* **`MODEL`**: `nirvaha-intelligence-v1`.

---

## 14. Data Provenance & Audit Traceability

Every `IntelligenceSignal` references:

* Underlying incident IDs.
* Meteorological observation IDs (where applicable).
* Administrative area ID.
* Generation timestamp and model version.
* Source provenance registry references.

---

## 15. Data Limitations & Graceful Handling

When data is incomplete or coordinates are missing:
* Returns `INSUFFICIENT_DATA` status.
* Does NOT fabricate fallback numbers or guess coordinates.

---

## 16. Simulated Data Isolation

`SIMULATED` test records are filtered out at the `IntelligenceEligibilityService` level before entering temporal, spatial, or trend calculations.

---

## 17. API Endpoints

All endpoints require JWT authentication and `OFFICER` or `ADMIN` RBAC privileges:

* `GET /api/intelligence/overview`
* `GET /api/intelligence/signals`
* `GET /api/intelligence/signals/{id}`
* `GET /api/intelligence/hotspots`
* `GET /api/intelligence/emerging-issues`
* `GET /api/intelligence/unresolved-load`
* `GET /api/intelligence/resolution-performance`

---

## 18. Authorization & RBAC

* Government Officers & Admins: Full access to municipal intelligence, spatial hotspots, and resolution performance.
* Citizens: Public incident tracking allowed; internal government intelligence, officer workflow details, and anomaly signals are restricted.

---

## 19. Testing Suite

The Phase 13 implementation includes unit, integration, and security tests:

* `IntelligenceEligibilityTest`: Validates exclusion of simulated and invalid records.
* `TrendAnalysisTest`: Verifies deterministic classification (`INCREASING`, `STABLE`, `DECREASING`, `INSUFFICIENT_DATA`).
* `HotspotDetectionTest`: Validates spatial clustering and category separation.
* `EmergingIssueTest`: Verifies surge percentage and volume thresholds.
* `IntelligenceSecurityTest`: Verifies 401/403 security enforcement on intelligence endpoints.

---

## 20. Known Limitations

* **Spatial Resolution**: Spatial clustering currently uses 500m Haversine distance centroids; high-density polygon intersections can be added in future phases.
* **Meteorological Coverage**: IMD rainfall context depends on active weather monitoring station proximity.
