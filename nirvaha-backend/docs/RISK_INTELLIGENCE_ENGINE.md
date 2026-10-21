# Phase 9 — Nirvaha Risk Intelligence Engine

## 1. Purpose & Product Principles
The **Nirvaha Risk Intelligence Engine** transforms reactive citizen complaint management into proactive municipal governance. It combines 6 explainable signal groups to estimate emerging civic risk across Puducherry wards and hotspots:

> **"Nirvaha Risk Intelligence predicts emerging civic risk and provides explainable recommendations for proactive municipal action."**
> **"Risk scores are decision-support signals, not guaranteed predictions."**

---

## 2. Architecture & Signal Aggregation Model

```text
Incident Data / Verification / Workflow
                   │
                   ▼
    RiskIntelligenceService (Java Engine)
                   │
  ┌────────────────┼────────────────┬────────────────┬────────────────┬────────────────┐
  ▼                ▼                ▼                ▼                ▼                ▼
Signal 1         Signal 2         Signal 3         Signal 4         Signal 5         Signal 6
Historical       Temporal         Recent Activity  Unresolved       Spatial          Environmental
Density          Recurrence       Time Decay       Load             Concentration    Weather
(20%)            (20%)            (e^-λt) (20%)    (20%)            (500m) (10%)     Open-Meteo (10%)
  │                │                │                │                │                │
  └────────────────┴────────────────┼────────────────┴────────────────┴────────────────┘
                                    │
                                    ▼
                         Score Normalization (0.0 to 1.0)
                                    │
                                    ▼
                   Risk Level Mapping (LOW/MOD/HIGH/CRIT)
                                    │
                                    ▼
                   Explainability & Recommendations
                                    │
                                    ▼
                     risk_assessments Table & REST APIs
```

---

## 3. Signal Groups & Weights

$$\text{RiskScore} = (s_1 \times 0.20) + (s_2 \times 0.20) + (s_3 \times 0.20) + (s_4 \times 0.20) + (s_5 \times 0.10) + (s_6 \times 0.10)$$

Where:
1. **Historical Incident Activity ($20\%$)**: Combined short-term (7-day) and medium-term (30-day) volume relative to baseline limits.
2. **Temporal Recurrence ($20\%$)**: Multi-month recurring incidents of the same category in the ward.
3. **Recent Activity / Time Decay ($20\%$)**: Exponential time decay formula: $w_i = e^{-\lambda \cdot t_{days}}$ with $\lambda = 0.1$. Today's incidents carry significantly higher weight than 30-day-old incidents.
4. **Unresolved Incident Load ($20\%$)**: Evaluates active incident burden (`REPORTED`, `VERIFIED`, `IN_PROGRESS`) and unverified Phase 7 resolutions (`PENDING`, `MANUAL_REVIEW`, `FAILED`).
5. **Spatial Concentration ($10\%$)**: Uses Phase 4 `HaversineDistance` to detect incident clusters within $500$ meters.
6. **Environmental / Weather Impact ($10\%$)**: Open-Meteo REST API (`https://api.open-meteo.com/v1/forecast`) with category-specific environmental relevance multipliers.

---

## 4. Category Weather Relevance Multipliers
Weather impact varies by category:
- **DRAINAGE**: $1.0$ (High relevance to rainfall accumulation)
- **POTHOLE**: $0.5$ (Moderate relevance to road degradation)
- **WATER_SUPPLY**: $0.3$ (Moderate relevance)
- **GARBAGE**: $0.1$ (Low relevance)
- **STREETLIGHT** / **OTHER**: $0.0$ (Zero weather weighting)

---

## 5. Risk Levels & Deterministic Boundaries

| Score Range | Risk Level | Municipal Interpretation |
| :---: | :---: | :--- |
| $0.00 - 0.24$ | `LOW` | Routine monitoring; standard maintenance schedule. |
| $0.25 - 0.49$ | `MODERATE` | Heightened awareness; schedule regular field inspections. |
| $0.50 - 0.74$ | `HIGH` | Elevated risk; prioritize field inspection and prep work. |
| $0.75 - 1.00$ | `CRITICAL` | Severe risk; deploy emergency maintenance teams immediately. |

---

## 6. Weather Fail-Safe Architecture
If Open-Meteo times out ($>2000$ms) or is unreachable:
- Risk calculation falls back to civic-only signals.
- Weather weight ($0.10$) is redistributed proportionally to civic factors so total weight remains $1.0$.
- Result explicitly states: *"Weather data unavailable; risk calculated from civic history and spatial signals."*

---

## 7. REST API Endpoints

### 1. Overview of Ward Risks
`GET /api/risk/wards`

### 2. Detailed Ward Risk
`GET /api/risk/wards/{wardId}?category=DRAINAGE`

### 3. Evaluate & Persist Risk Assessments
`POST /api/risk/assessments/evaluate`

---

## 8. Database Schema

```sql
CREATE TABLE wards (
    id BIGSERIAL PRIMARY KEY,
    ward_number INT NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    latitude NUMERIC(10,7) NOT NULL,
    longitude NUMERIC(10,7) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE risk_assessments (
    id BIGSERIAL PRIMARY KEY,
    ward_id BIGINT NOT NULL REFERENCES wards(id),
    category VARCHAR(255) NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(255) NOT NULL,
    historical_score DOUBLE PRECISION NOT NULL,
    recent_activity_score DOUBLE PRECISION NOT NULL,
    recurrence_score DOUBLE PRECISION NOT NULL,
    unresolved_load_score DOUBLE PRECISION NOT NULL,
    spatial_concentration_score DOUBLE PRECISION NOT NULL,
    weather_impact_score DOUBLE PRECISION NOT NULL,
    weather_context VARCHAR(500),
    explanation VARCHAR(2000) NOT NULL,
    recommended_action VARCHAR(1000),
    assessment_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

---

## 9. Future Extensions
- **PostGIS Integration**: Extension path to replace Ward center coordinates with official PostGIS polygon boundary geometry.
- **Python AI Extension**: `RiskPredictionService` Java interface provides a clean contract to delegate time-series ML forecasting to a Python FastAPI microservice without breaking domain models or REST contracts.
