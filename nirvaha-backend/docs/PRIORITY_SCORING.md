# Explainable Priority Scoring Engine

## 1. Purpose of the Priority Scoring Engine
The Priority Scoring Engine dynamically evaluates civic incident reports submitted to the Nirvaha platform and assigns a quantitative **Priority Score** (0.0 to 100.0) and a **Priority Level** (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). 

Its primary objective is to assist municipal officers and emergency response teams in identifying high-impact civic issues (e.g., severe sewage overflows, hazardous road collapses, critical water main bursts) and dispatching resources efficiently.

---

## 2. Why Deterministic Rules are Used
For Stage I (State Level submission), the priority scoring engine uses **100% deterministic, rule-based mathematical formulas** rather than opaque machine learning models or black-box AI algorithms.

### Benefits of Determinism:
- **Auditability & Defense**: Every score calculated can be audited, defended, and validated before municipal judges and officers.
- **Predictability**: Identical inputs always yield the exact same score and priority classification.
- **Transparency**: Municipal officers can inspect the exact component factors and human-readable reasons explaining *why* an incident received its priority.
- **Zero Hallucination Risk**: Eliminates unpredictable scoring variations typical of LLM-based black-box evaluators.

---

## 3. Core Input Factors

The Priority Scoring Engine evaluates four primary factors derived directly from the Nirvaha data model:

| Factor Name | Description | Weight |
| :--- | :--- | :---: |
| **Incident Severity** | Standard severity level declared upon incident creation (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). | **40%** |
| **Report / Duplicate Volume** | Total count of reports representing the same civic issue (target incident + verified duplicates). | **30%** |
| **Location / Local Impact** | Density of nearby incidents of the **same category** within a 500-meter physical radius. | **20%** |
| **Environmental / Rainfall** | Optional environmental impact factor based on local rainfall levels (in mm). | **10%** |

---

## 4. Severity Factor Mapping

Incident severity is mapped deterministically to a normalized factor score between `0.00` and `1.00`:

| `IncidentSeverity` Enum | Factor Score | Explanation Reason |
| :--- | :---: | :--- |
| `LOW` | `0.25` | "Incident severity is LOW" |
| `MEDIUM` | `0.50` | "Incident severity is MEDIUM" |
| `HIGH` | `0.75` | "Incident severity is HIGH" |
| `CRITICAL` | `1.00` | "Incident severity is CRITICAL" |

---

## 5. Report Volume Factor Mapping

Report volume leverages the Phase 4 `DuplicateDetectionService` to count verified duplicate reports for the target incident. Total volume equals `1 (target incident) + count of duplicate matches`.

| Total Reports | Factor Score | Explanation Reason |
| :---: | :---: | :--- |
| **1 Report** | `0.00` | "Single report filed for this incident" |
| **2 Reports** | `0.50` | "2 reports indicate the same civic issue" |
| **3+ Reports** | `1.00` | "Multiple reports (N) indicate the same civic issue" |

---

## 6. Location / Local Impact Mapping

Local impact measures physical incident density. To avoid treating unrelated issues (e.g. garbage + streetlight + pothole) as a single cluster, local impact **strictly filters for incidents of the SAME category** within `500.0 meters` using Haversine distance calculations.

| Nearby Same-Category Incidents | Factor Score | Explanation Reason |
| :---: | :---: | :--- |
| **0 Nearby** | `0.00` | "No nearby related incidents reported" |
| **1 – 2 Nearby** | `0.50` | "Nearby related incidents increase local impact" |
| **3+ Nearby** | `1.00` | "Dense cluster of nearby related incidents (N) significantly increases local impact" |

---

## 7. Environmental / Rainfall Factor Mapping

The rainfall factor provides architecture for environmental risk escalation (e.g. heavy rain aggravating drainage or pothole hazards). When rainfall data is unavailable, the factor safely defaults to `0.00` without impacting core business evaluation.

| Rainfall Range (mm) | Factor Score | Explanation Reason |
| :---: | :---: | :--- |
| `null` / Unavailable | `0.00` | "Rainfall data unavailable; no environmental contribution applied" |
| `0.0 mm` | `0.00` | "No rainfall detected (0.0 mm); normal weather conditions" |
| `> 0.0` to `10.0 mm` | `0.25` | "Light rainfall detected (N mm)" |
| `> 10.0` to `35.0 mm` | `0.50` | "Moderate rainfall detected (N mm)" |
| `> 35.0` to `75.0 mm` | `0.75` | "Heavy rainfall detected (N mm)" |
| `> 75.0 mm` | `1.00` | "Extreme rainfall detected (N mm)" |

---

## 8. Weight Configuration

The scoring formula assigns the following weights to sum to 100%:

$$w_{\text{severity}} = 0.40 \quad (40\%)$$
$$w_{\text{volume}} = 0.30 \quad (30\%)$$
$$w_{\text{impact}} = 0.20 \quad (20\%)$$
$$w_{\text{rainfall}} = 0.10 \quad (10\%)$$

---

## 9. Mathematical Formula

The final **Priority Score** is computed dynamically on a `0.0` to `100.0` scale:

$$\text{PriorityScore} = \left( (f_{\text{severity}} \times 0.40) + (f_{\text{volume}} \times 0.30) + (f_{\text{impact}} \times 0.20) + (f_{\text{rainfall}} \times 0.10) \right) \times 100.0$$

*The score is rounded to 1 decimal place using `RoundingMode.HALF_UP`.*

---

## 10. Priority Level Thresholds

The calculated score maps to a human-understandable `PriorityLevel`:

| Priority Score Range | `PriorityLevel` | Municipal Action |
| :---: | :---: | :--- |
| **0.0 – 24.9** | `LOW` | Standard routine maintenance queue. |
| **25.0 – 49.9** | `MEDIUM` | Scheduled inspection within standard SLA window. |
| **50.0 – 74.9** | `HIGH` | High priority dispatch; field inspection prioritized. |
| **75.0 – 100.0** | `CRITICAL` | Immediate emergency response and municipal escalation. |

---

## 11. Example Calculation

### Inputs:
- `IncidentSeverity`: `CRITICAL` ($f_{\text{severity}} = 1.00$)
- Duplicate Matches: 2 duplicates ($f_{\text{volume}} = 1.00$)
- Nearby Same-Category Incidents: 1 nearby ($f_{\text{impact}} = 0.50$)
- Rainfall: Unavailable ($f_{\text{rainfall}} = 0.00$)

### Step-by-Step Step Evaluation:
$$\text{Weighted Score} = (1.00 \times 0.40) + (1.00 \times 0.30) + (0.50 \times 0.20) + (0.00 \times 0.10)$$
$$\text{Weighted Score} = 0.40 + 0.30 + 0.10 + 0.00 = 0.80$$
$$\text{PriorityScore} = 0.80 \times 100.0 = 80.0$$
$$\text{PriorityLevel} = \text{CRITICAL} \quad (\text{since } 80.0 \ge 75.0)$$

---

## 12. Example API Response

### Endpoint
`GET /api/incidents/46/priority`

### HTTP 200 OK Response:
```json
{
  "incidentId": 46,
  "priorityScore": 80.0,
  "priorityLevel": "CRITICAL",
  "factors": {
    "severityFactor": 1.0,
    "reportVolumeFactor": 1.0,
    "locationImpactFactor": 0.5,
    "rainfallFactor": 0.0
  },
  "reasons": [
    "Incident severity is CRITICAL",
    "Multiple reports (3) indicate the same civic issue",
    "Nearby related incidents increase local impact",
    "Rainfall data unavailable; no environmental contribution applied"
  ]
}
```

### HTTP 404 Not Found Response:
```json
{
  "timestamp": "2026-09-09T15:34:51.1304634",
  "status": 404,
  "error": "Not Found",
  "message": "Incident with ID 999999 not found"
}
```

---

## 13. Explainability Design
Explainability is built into the response payload via the `reasons` list. Rather than returning raw numerical scores alone, each factor append a human-readable sentence explaining its contribution. Officers viewing the Nirvaha administrative dashboard immediately understand *why* an incident reached `CRITICAL` or `HIGH` priority without needing technical background in the formula.

---

## 14. Why No ML is Used for This Phase
1. **No Training Data Dependency**: Civic incident priority scoring in new municipal regions lacks labeled historical data.
2. **Defendability**: Machine learning models can produce unexpected outputs under edge cases, making them difficult to justify during municipal hackathon evaluations.
3. **Zero Cold-Start Latency**: Deterministic calculation runs in sub-millisecond execution times without heavy model loading overhead.

---

## 15. Future Extension Points
In future stages (Phase 6+):
- **Real-Time Weather Integration**: An external weather service integration (e.g. OpenWeatherMap API) can populate the optional `rainfall` parameter based on latitude/longitude coordinates.
- **Dynamic Weight Adjustments**: Weights can be configured per municipal zone via externalized Spring properties (`@ConfigurationProperties`).
