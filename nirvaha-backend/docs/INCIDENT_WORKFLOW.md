# Government Action & Resolution Workflow

## 1. Purpose
The Government Action & Resolution Workflow provides an auditable, deterministic, and transparent lifecycle tracking layer for civic incidents managed within the Nirvaha platform.

It answers three core municipal governance questions:
1. *"What happened to this civic complaint after it was reported?"*
2. *"Which government officer is responsible for it now?"*
3. *"What actions were taken before it was resolved?"*

---

## 2. Architecture

```text
Citizen
   │
   ▼
Incident (REPORTED)
   │
   ├── Duplicate Detection (Phase 4)
   │
   ├── Priority Scoring (Phase 5)
   │
   └── Government Workflow (Phase 6)
          │
          ├── ASSIGNED
          ├── VERIFIED
          ├── STARTED
          ├── PROGRESS_UPDATE
          ├── RESOLVED
          └── REOPENED
```

The `Incident` entity remains the primary civic problem. The workflow layer creates an append-only, immutable-style audit trail (`incident_actions` table) recording every administrative action taken by government officers without breaking or replacing existing Phase 1–5 data structures.

---

## 3. Incident Lifecycle

```text
REPORTED
   │
   ├── ASSIGNED  ──► (Status remains REPORTED or VERIFIED)
   │
   ├── VERIFIED  ──► (Status → VERIFIED)
   │
   ├── STARTED   ──► (Status → IN_PROGRESS)
   │
   ├── PROGRESS_UPDATE ──► (Status remains IN_PROGRESS)
   │
   └── RESOLVED  ──► (Status → RESOLVED)
          │
          └── REOPENED ──► (Status → REPORTED)
```

---

## 4. Action Types

| `ActionType` Enum | Primary Purpose | Effect on `IncidentStatus` | Remarks Requirement |
| :--- | :--- | :--- | :--- |
| `ASSIGNED` | Assigns an officer/department to the incident | Unchanged (`REPORTED`, `VERIFIED`, `IN_PROGRESS`) | Optional |
| `VERIFIED` | Field inspection confirms valid civic issue | `VERIFIED` | Optional |
| `STARTED` | Work crew or machinery dispatched to site | `IN_PROGRESS` | Optional |
| `PROGRESS_UPDATE` | Intermediary progress log during resolution | Unchanged (`IN_PROGRESS`) | Optional |
| `RESOLVED` | Final resolution of the civic issue | `RESOLVED` | **Mandatory** |
| `REOPENED` | Re-activates a recurring/unresolved issue | `REPORTED` | **Mandatory** |

---

## 5. State Transition Rules

The system enforces strict, deterministic transitions to protect data integrity:

1. **`ASSIGNED`**:
   - Updates assigned officer tracking.
   - Preserves current status (`REPORTED`, `VERIFIED`, `IN_PROGRESS`).
   - Cannot be applied directly to a `RESOLVED` incident without explicit reopening.

2. **`VERIFIED`**:
   - Transitions status from `REPORTED` -> `VERIFIED`.

3. **`STARTED`**:
   - Transitions status -> `IN_PROGRESS`.

4. **`PROGRESS_UPDATE`**:
   - **Requires** current status to be `IN_PROGRESS`.
   - Attempts to post updates when status is `REPORTED`, `VERIFIED`, or `RESOLVED` are rejected with `400 Bad Request`.

5. **`RESOLVED`**:
   - Transitions status -> `RESOLVED`.
   - **Requires non-empty remarks** explaining the resolution work performed.
   - Attempts to resolve without remarks are rejected with `400 Bad Request`.

6. **`REOPENED`**:
   - Transitions status from `RESOLVED` -> `REPORTED`.
   - **Requires non-empty remarks** explaining why the issue is being reopened.
   - Attempts to reopen non-resolved incidents are rejected with `400 Bad Request`.

---

## 6. Officer Validation
- Government actions can **only** be posted by users with `OFFICER` or `ADMIN` roles.
- If a user with role `CITIZEN` attempts to post a workflow action, the request is rejected with `HTTP 400 Bad Request`:
  `"User with ID X is not authorized to perform government incident actions"`

---

## 7. Assignment Logic & Current Officer Determination
- An incident can undergo multiple reassignments over its lifecycle (`Officer A -> Officer B`).
- The current responsible officer is derived dynamically from the **latest `ASSIGNED` action** (`createdAt DESC`).
- This guarantees auditability while accurately displaying the responsible officer on municipal dashboards (`assignedOfficerId`, `assignedOfficerName`).

---

## 8. Append-Only Timeline Model
- Workflow actions are strictly **append-only**.
- No endpoints exist to edit or delete historical actions.
- The `GET /api/incidents/{incidentId}/actions` and `GET /api/incidents/{incidentId}/workflow` endpoints return historical actions in **chronological order** (`createdAt ASC`).

---

## 9. API Endpoints

### 1. Post Workflow Action
`POST /api/incidents/{incidentId}/actions`

**Request Body**:
```json
{
  "officerId": 4,
  "actionType": "ASSIGNED",
  "remarks": "Assigned to Puducherry Drainage Response Unit."
}
```

**Response (201 Created)**:
```json
{
  "id": 16,
  "incidentId": 129,
  "officerId": 4,
  "officerName": "Officer Bob",
  "actionType": "ASSIGNED",
  "remarks": "Assigned to Puducherry Drainage Response Unit.",
  "createdAt": "2026-09-09T15:56:33.249062"
}
```

---

### 2. Get Chronological Action History
`GET /api/incidents/{incidentId}/actions`

**Response (200 OK)**:
```json
[
  {
    "id": 16,
    "incidentId": 129,
    "officerId": 4,
    "officerName": "Officer Bob",
    "actionType": "ASSIGNED",
    "remarks": "Assigned to Puducherry Drainage Response Unit.",
    "createdAt": "2026-09-09T15:56:33.249062"
  },
  {
    "id": 17,
    "incidentId": 129,
    "officerId": 4,
    "officerName": "Officer Bob",
    "actionType": "VERIFIED",
    "remarks": "Field inspection confirmed 20m overflow.",
    "createdAt": "2026-09-09T15:56:33.301153"
  }
]
```

---

### 3. Get Complete Workflow Timeline
`GET /api/incidents/{incidentId}/workflow`

**Response (200 OK)**:
```json
{
  "incidentId": 129,
  "currentStatus": "RESOLVED",
  "assignedOfficerId": 4,
  "assignedOfficerName": "Officer Bob",
  "actions": [
    {
      "id": 16,
      "incidentId": 129,
      "officerId": 4,
      "officerName": "Officer Bob",
      "actionType": "ASSIGNED",
      "remarks": "Assigned to Puducherry Drainage Response Unit.",
      "createdAt": "2026-09-09T15:56:33.249062"
    },
    {
      "id": 20,
      "incidentId": 129,
      "officerId": 4,
      "officerName": "Officer Bob",
      "actionType": "RESOLVED",
      "remarks": "Drainage blockage completely cleared and normal flow restored.",
      "createdAt": "2026-09-09T15:56:33.359231"
    }
  ]
}
```

---

### 4. Error Responses

**Unauthorized Citizen Action (400 Bad Request)**:
```json
{
  "timestamp": "2026-09-09T15:56:33.4459588",
  "status": 400,
  "error": "Bad Request",
  "message": "User with ID 225 is not authorized to perform government incident actions"
}
```

**Non-Existent Incident (404 Not Found)**:
```json
{
  "timestamp": "2026-09-09T15:56:33.5188228",
  "status": 404,
  "error": "Not Found",
  "message": "Incident with ID 999999 not found"
}
```

---

## 10. Database Schema (`incident_actions`)

```sql
CREATE TABLE incident_actions (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id),
    officer_id BIGINT NOT NULL REFERENCES users(id),
    action_type VARCHAR(255) NOT NULL,
    remarks VARCHAR(2000),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

---

## 11. Testing Strategy
- Unit & integration tests in `IncidentWorkflowServiceTest` (14 test cases) and `IncidentWorkflowControllerTest` (5 test cases).
- Verification of:
  - All valid state transitions (`ASSIGNED`, `VERIFIED`, `STARTED`, `PROGRESS_UPDATE`, `RESOLVED`, `REOPENED`).
  - Rejection of invalid status transitions.
  - Rejection of citizen role execution.
  - Rejection of missing/empty remarks for `RESOLVED` and `REOPENED`.
  - Derivation of latest assigned officer.
  - Chronological ordering of action history.

---

## 12. Future Extension Points (Phase 7+)
- **SLA & Escalation Notifications**: Automatic alerts triggered if an incident remains in `ASSIGNED` or `IN_PROGRESS` past standard response SLAs.
- **Role-Based Access Control**: Integration with Spring Security & JWT for authenticated header tokens.
