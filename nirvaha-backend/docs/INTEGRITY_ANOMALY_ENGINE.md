# Phase 8 — Integrity & Anomaly Detection Engine

## 1. Purpose
The **Integrity & Anomaly Detection Engine** provides objective, explainable, and auditable anomaly detection for civic incident data in the Nirvaha platform.

It addresses a fundamental municipal governance problem:
> *"Unusual or suspicious patterns in civic incident reporting and resolution must be identified early and routed to human officers for verification before administrative tickets are closed without audit."*

### Core Product Principle
> **Nirvaha detects anomalies and recommends review. Humans make the final decision.**

Nirvaha is an integrity-support system, not a corruption accusation tool. It flags statistical and pattern anomalies objectively without judging intent or making accusations of misconduct.

---

## 2. Architecture & Design Principles

```text
Citizen Reports / Resolution Actions
                 │
                 ▼
    IntegrityAnomalyService (Java Engine)
                 │
  ┌──────────────┼──────────────┐
  ▼              ▼              ▼
Rule 1         Rule 2         Rule 3
Evidence       Bulk           Repeated
Reuse          Closure        Reporting
  │              │              │
  ▼              ▼              ▼
HIGH           MEDIUM         LOW
Severity       Severity       Severity
  │              │              │
  └──────────────┼──────────────┘
                 │
                 ▼
  Deduplication & Uniqueness Guard
                 │
                 ▼
         anomaly_flags Table
                 │
                 ▼
   REST API / Governance Dashboard
```

1. **Deterministic & Explainable**: Every anomaly flag is generated via transparent threshold rules. No black-box ML or AI models are used.
2. **Auditability**: Anomaly flags are persisted in `anomaly_flags` with timestamped audit context and related incident IDs.
3. **Idempotency & Deduplication**: Calling the detection endpoint repeatedly for the same incident updates existing flags without inserting duplicate database records.

---

## 3. Anomaly Rules & Thresholds

| Rule | Anomaly Type | Condition / Threshold | Severity | Human-Readable Description Template |
| :--- | :--- | :--- | :---: | :--- |
| **Rule 1** | `RESOLUTION_EVIDENCE_REUSE` | Exact same resolution evidence reference (`fileUrl`) associated with resolutions across different incidents. | `HIGH` | *"The same resolution evidence reference is associated with multiple incidents. Review recommended."* |
| **Rule 2** | `BULK_CLOSURE` | Same officer resolves $\ge 5$ incidents within a rolling 60-minute window (`BULK_CLOSURE_THRESHOLD = 5`, `BULK_CLOSURE_WINDOW_MINUTES = 60`). | `MEDIUM` | *"{count} incidents were marked resolved by the same officer within 60 minutes. Review recommended."* |
| **Rule 3** | `REPEATED_REPORTING` | Same category, distance $\le 500$ meters, time difference $\le 24$ hours ($1440$ mins), count $\ge 3$ incidents. | `LOW` | *"{count} similar {category} reports were submitted within 500 metres and 24 hours. Review recommended."* |

---

## 4. Anomaly Domain Enums & Entity

### Enums
- `AnomalyType`: `RESOLUTION_EVIDENCE_REUSE`, `BULK_CLOSURE`, `REPEATED_REPORTING`
- `AnomalySeverity`: `LOW`, `MEDIUM`, `HIGH`
- `AnomalyStatus`: `OPEN`, `REVIEWED`, `DISMISSED`

### Entity Schema (`anomaly_flags`)
```sql
CREATE TABLE anomaly_flags (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id),
    anomaly_type VARCHAR(255) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    related_incident_ids VARCHAR(500),
    status VARCHAR(255) NOT NULL DEFAULT 'OPEN',
    detected_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

---

## 5. REST API Endpoints

### 1. Get Anomalies for Incident
`GET /api/incidents/{incidentId}/anomalies`

**Response Payload (200 OK)**:
```json
{
  "incidentId": 129,
  "anomalies": [
    {
      "id": 1,
      "incidentId": 129,
      "anomalyType": "BULK_CLOSURE",
      "severity": "MEDIUM",
      "description": "6 incidents were marked resolved by the same officer within 60 minutes. Review recommended.",
      "relatedIncidentIds": "124,125,126,127,128",
      "status": "OPEN",
      "detectedAt": "2026-09-10T10:45:00"
    }
  ]
}
```

### 2. Trigger Anomaly Detection
`POST /api/incidents/{incidentId}/anomalies/detect`

Evaluates target incident against all 3 anomaly rules, persists new/updated flags idempotently, and returns the active flags.

**Response Payload (200 OK)**:
```json
{
  "incidentId": 201,
  "anomalies": [
    {
      "id": 2,
      "incidentId": 201,
      "anomalyType": "REPEATED_REPORTING",
      "severity": "LOW",
      "description": "3 similar drainage reports were submitted within 500 metres and 24 hours. Review recommended.",
      "relatedIncidentIds": "202,203",
      "status": "OPEN",
      "detectedAt": "2026-09-10T10:45:00"
    }
  ]
}
```

### 3. Get Anomalies for Officer (Optional Dashboard Endpoint)
`GET /api/officers/{officerId}/anomalies`

Returns all anomaly flags associated with incidents resolved by the specified officer.

---

## 6. Language & Ethical Guidelines
All descriptions, API responses, logs, and documentation strictly follow objective integrity messaging:
- **Allowed Terms**: *"anomaly detected"*, *"unusual pattern"*, *"evidence reuse"*, *"review recommended"*, *"manual review required"*.
- **Forbidden Terms**: *"fraud"*, *"corruption"*, *"fake"*, *"misconduct"*, *"criminal"*, *"officer lied"*, *"citizen lied"*.

---

## 7. Testing & Verification

The suite tests all rules, edge cases, deduplication, and HTTP status codes:
- **Service Unit & Integration Tests**: `IntegrityAnomalyServiceTest` (14 test cases covering reuse, bulk closure limits, spatial/temporal thresholds, duplicate prevention, and validation).
- **Controller Unit Tests**: `IntegrityAnomalyControllerTest` (4 test cases covering GET/POST endpoints, 200 OK responses, and 404 Not Found error mapping).
- **Full Regression**: Verified 100% pass rate across Phase 1 through Phase 8 tests.

---

## 8. Limitations & Future Extensions
- **Resolution Evidence Identity**: The Phase 8 MVP detects evidence reuse based on identical evidence URI references (`fileUrl`). It does not claim computer-vision image hash comparisons unless an explicit image hashing microservice is integrated.
- **Future Extension Points** (not implemented in Phase 8):
  - Month-end closure spike detection
  - Perceptual image hashing (pHash / SSIM) via Python service
  - Impossible travel time detection for officers
