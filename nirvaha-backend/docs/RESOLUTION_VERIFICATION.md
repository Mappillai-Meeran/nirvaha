# Resolution Evidence & Verification Engine

## 1. Purpose
The Resolution Evidence & Verification Engine provides automated, objective evidence-based verification for civic incident closures in the Nirvaha platform.

It addresses the fundamental municipal governance problem:
> *"An incident marked RESOLVED by a government officer should not be treated as a trustworthy closure without verifiable visual evidence."*

---

## 2. Problem Being Solved
In traditional municipal complaint management systems, closing an issue relies solely on an officer updating a status field to `RESOLVED`. This creates a vulnerability where tickets can be closed administratively without physical repair work being completed.

Nirvaha solves this by establishing a verifiable chain of trust:
$$\text{Citizen Evidence} \longrightarrow \text{Government Action} \longrightarrow \text{Resolution Evidence} \longrightarrow \text{Verification Status}$$

---

## 3. Architecture

```text
Citizen Report
   │
   ▼
Original Evidence (Photo/Video/Metadata)
   │
   ▼
Government Workflow (ASSIGNED ──► VERIFIED ──► IN_PROGRESS)
   │
   ▼
Officer Submits Resolution Evidence
   │
   ▼
ResolutionComparisonService (Java Abstraction)
   │
   ├── Spatial Proximity Evaluation (Haversine Distance)
   ├── Media Format Compatibility (PHOTO vs PHOTO)
   └── Metadata/Location Alignment
   │
   ▼
resolutionSimilarityScore (0.0 to 1.0)
   │
   ▼
Verification Decision Engine
   ├── score >= 0.80  ──► VERIFIED (Incident status updated to RESOLVED)
   ├── score >= 0.50  ──► MANUAL_REVIEW
   └── score < 0.50   ──► FAILED
```

---

## 4. Resolution Workflow

```text
IN_PROGRESS / VERIFIED
   │
   ▼
Submit Resolution Evidence (POST /api/incidents/{id}/resolutions)
   │
   ├── Check Officer Authorization (OFFICER or ADMIN)
   ├── Validate Evidence Ownership (belongs to incident)
   ├── Identify Original Citizen Evidence
   ├── Compute resolutionSimilarityScore
   └── Evaluate VerificationStatus
          │
          ├── VERIFIED ──► Automatically sets Incident status to RESOLVED
          ├── MANUAL_REVIEW ──► Requires admin review
          └── FAILED ──► Incident remains IN_PROGRESS
```

---

## 5. Verification Statuses (`VerificationStatus` Enum)

| Status | Meaning | Municipal Action |
| :--- | :--- | :--- |
| `PENDING` | Resolution evidence submitted but pending evaluation. | Queue for processor. |
| `VERIFIED` | Resolution evidence strongly supports closure. | Incident status automatically updated to `RESOLVED`. |
| `MANUAL_REVIEW` | Moderate similarity detected; requires manual inspection. | Flagged for administrative oversight. |
| `FAILED` | Insufficient evidence similarity to confirm resolution. | Issue remains open (`IN_PROGRESS`). |

---

## 6. Resolution Similarity Score (`resolutionSimilarityScore`)
- Exact property name: `resolutionSimilarityScore`
- Value range: Normalized strictly between `0.0` and `1.0`.
- Computation formula in MVP baseline:
$$\text{Score} = (s_{\text{proximity}} \times 0.50) + (s_{\text{media}} \times 0.30) + (s_{\text{metadata}} \times 0.20)$$

---

## 7. Verification Thresholds

| Threshold Condition | `VerificationStatus` | Explanation Wording |
| :--- | :---: | :--- |
| $\text{score} \ge 0.80$ | `VERIFIED` | *"Evidence supports resolution."* |
| $0.50 \le \text{score} < 0.80$ | `MANUAL_REVIEW` | *"Moderate visual similarity detected. Manual review recommended."* |
| $\text{score} < 0.50$ | `FAILED` | *"Insufficient evidence to verify resolution."* |

---

## 8. REST API Endpoints

### 1. Submit Resolution Evidence
`POST /api/incidents/{incidentId}/resolutions`

**Request Payload**:
```json
{
  "officerId": 394,
  "evidenceId": 96,
  "remarks": "Asphalt patch applied and road surface leveled."
}
```

**Response Payload (201 Created)**:
```json
{
  "id": 11,
  "incidentId": 312,
  "officerId": 394,
  "officerName": "Officer Dan",
  "resolutionEvidenceId": 96,
  "resolutionEvidenceUrl": "http://storage.puducherry.gov.in/evidence/repaired_pothole.jpg",
  "resolutionSimilarityScore": 1.0,
  "verificationStatus": "VERIFIED",
  "remarks": "Evidence supports resolution (similarity score: 1.00). Remarks: Asphalt patch applied and road surface leveled.",
  "createdAt": "2026-09-09T16:17:31.8681666",
  "updatedAt": "2026-09-09T16:17:31.8681666"
}
```

---

### 2. Retrieve Resolution Verification
`GET /api/incidents/{incidentId}/resolution`

**Response Payload (200 OK)**:
```json
{
  "id": 11,
  "incidentId": 312,
  "officerId": 394,
  "officerName": "Officer Dan",
  "resolutionEvidenceId": 96,
  "resolutionEvidenceUrl": "http://storage.puducherry.gov.in/evidence/repaired_pothole.jpg",
  "resolutionSimilarityScore": 1.0,
  "verificationStatus": "VERIFIED",
  "remarks": "Evidence supports resolution (similarity score: 1.00). Remarks: Asphalt patch applied and road surface leveled.",
  "createdAt": "2026-09-09T16:17:31.868167",
  "updatedAt": "2026-09-09T16:17:31.868167"
}
```

---

## 9. Database Schema (`resolutions`)

```sql
CREATE TABLE resolutions (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id),
    officer_id BIGINT NOT NULL REFERENCES users(id),
    resolution_evidence_id BIGINT NOT NULL REFERENCES evidence(id),
    resolution_similarity_score DOUBLE PRECISION NOT NULL,
    verification_status VARCHAR(255) NOT NULL,
    remarks VARCHAR(2000),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

---

## 10. Validation Rules
1. **Officer Authorization**: User submitting resolution must have role `OFFICER` or `ADMIN`. `CITIZEN` users are rejected (`HTTP 400 Bad Request`).
2. **Evidence Ownership**: Submitted resolution evidence MUST belong to `incidentId`. Evidence belonging to another incident is rejected (`HTTP 400 Bad Request`).
3. **Workflow State**: Resolution evidence cannot be submitted for unverified `REPORTED` incidents (`HTTP 400 Bad Request`).
4. **Resource Verification**: Non-existent incident, officer, or evidence returns `HTTP 404 Not Found`.

---

## 11. Testing Strategy
- Unit & integration test suites in `ResolutionServiceTest` (12 test cases) and `ResolutionControllerTest` (4 test cases).
- Full regression verification: **82 total tests passing (0 failures, 0 errors, 0 skipped)**.

---

## 12. Limitations
- The Java MVP similarity comparison evaluates spatial proximity, media type, and metadata alignment.
- It provides evidence-assisted verification guidelines rather than claiming 100% computer-vision proof.

---

## 13. Future Python AI `/compare` Integration
`ResolutionComparisonService` is designed as an isolated Java service component. In Stage II, a Python FastAPI AI microservice will provide an endpoint:
`POST /compare` (accepting original image bytes & resolution image bytes and returning SSIM / feature similarity scores).

The Java `ResolutionComparisonService` can be updated to call `POST /compare` via REST without changing any domain models, database schema, `ResolutionService`, or `ResolutionController` contracts.
