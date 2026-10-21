# Nirvaha Phase 3 — Resident Resolution Confirmation & Dispute Workflow Report

**Document Version:** 1.0.0  
**Project Root:** `E:\Nirvaha`  
**Backend:** `E:\Nirvaha\nirvaha-backend` (Spring Boot 3.4.3 / Java 21 / PostgreSQL / Hibernate)  
**Frontend:** `E:\Nirvaha\nirvaha-frontend` (React 19 / Vite / TailwindCSS / Lucide)  
**Audit Reference:** [PRODUCT_RELEASE_READINESS_AUDIT.md](file:///E:/Nirvaha/PRODUCT_RELEASE_READINESS_AUDIT.md) (Gaps 3 & 4)  
**Status:** **IMPLEMENTED, SECURED & AUTOMATED TESTS PASSING (252/252 BACKEND TESTS)**

---

## 1. Executive Summary

In Nirvaha Phase 3, we implemented an auditable, cryptographically traceable resident feedback workflow for municipal incidents in status `RESOLVED`. Prior to Phase 3, once an officer marked an incident resolved, the genuine reporting citizen was locked out of the workflow—unable to confirm the repair, reopen a lingering issue, or contest substandard workmanship without administrative credentials.

Phase 3 establishes full closed-loop accountability with strict safeguards:
1. **Original Reporter Authentication**: Only the authenticated citizen who originally reported the incident (`incident.reporter.id == currentUser.id`) can submit feedback.
2. **Impersonation Prevention**: Officers and administrators cannot use resident feedback endpoints (`HTTP 403 Forbidden`).
3. **Cross-Resident Isolation**: A resident attempting to provide feedback on another resident's incident is denied (`HTTP 403 Forbidden`).
4. **Resolution Evidence Preservation**: When a resident disputes a resolution, the original resolution record, remediation photograph, SHA-256 content checksum, similarity score, and officer attribution are **never overwritten or deleted**.
5. **Reopening State Lifecycle**: A disputed incident is automatically routed into Nirvaha's supported supervisory review workflow (`IncidentStatus.REPORTED`).
6. **Authorized Officer Dispute Review**: An authorized officer (`OFFICER` or `ADMIN`) must formally review and dispose of the dispute (`POST /api/incidents/{id}/feedback/{feedbackId}/review`) with documented supervisor findings.

---

## 2. Architecture & Domain Model

### 2.1 Database Schema & Flyway Migration

Flyway migration script `V3__resident_feedback_and_disputes.sql` was added to `nirvaha-backend/src/main/resources/db/migration/`:

```sql
-- V3__resident_feedback_and_disputes.sql
CREATE TABLE incident_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    resident_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    resolution_id BIGINT REFERENCES resolutions(id) ON DELETE SET NULL,
    feedback_type VARCHAR(32) NOT NULL,
    explanation TEXT,
    status VARCHAR(32) NOT NULL,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    review_notes TEXT,
    demo_run_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_incident_feedbacks_incident ON incident_feedbacks(incident_id);
CREATE INDEX idx_incident_feedbacks_resident ON incident_feedbacks(resident_id);
CREATE INDEX idx_incident_feedbacks_demo_run ON incident_feedbacks(demo_run_id);
```

### 2.2 PostgreSQL Check Constraint Alignment

PostgreSQL enforces a database-level check constraint on `incident_actions.action_type`. To allow new immutable audit types, the constraint was modified in the local database:
```sql
ALTER TABLE incident_actions DROP CONSTRAINT incident_actions_action_type_check;
ALTER TABLE incident_actions ADD CONSTRAINT incident_actions_action_type_check 
  CHECK (action_type IN (
    'REPORTED', 'ASSIGNED', 'STARTED', 'PROGRESS_UPDATE', 
    'RESOLVED', 'VERIFIED', 'REOPENED', 
    'RESIDENT_CONFIRMED', 'RESIDENT_DISPUTED', 'DISPUTE_REVIEWED'
  ));
```

### 2.3 Domain Entities & Enums

- **`FeedbackType`**: `CONFIRMED`, `DISPUTED`
- **`ActionType`**: Added `RESIDENT_CONFIRMED`, `RESIDENT_DISPUTED`, `DISPUTE_REVIEWED`
- **`IncidentFeedback`**: Links `incident`, `resident`, `resolution`, `reviewedBy`, tracking `status`, `explanation`, `reviewed`, `reviewNotes`, and `demoRunId`.
- **`IncidentFeedbackRepository`**: Enables scoped lookups by incident ID and demo run ID.

---

## 3. API Contracts

### 3.1 Resident Submits Feedback (Confirmation or Dispute)

- **Endpoint**: `POST /api/incidents/{incidentId}/feedback`
- **Authorization**: `ROLE_CITIZEN` only (enforced by Spring Security and `ResidentFeedbackService`)
- **Request Headers**: `Authorization: Bearer <jwt>`
- **Request Body**:
  ```json
  {
    "feedbackType": "CONFIRMED | DISPUTED",
    "explanation": "Mandatory explanation for disputes; optional for confirmations (max 2000 chars)"
  }
  ```
- **Responses**:
  - `201 Created`: Feedback recorded. Returns `IncidentFeedbackResponse`.
  - `400 Bad Request`: Incident not in `RESOLVED` status, or blank explanation for dispute.
  - `401 Unauthorized`: Missing or invalid JWT.
  - `403 Forbidden`: Authenticated user is not the incident reporter, or caller is an officer/admin.
  - `409 Conflict`: Resident has already submitted feedback for this incident.

### 3.2 Fetch Resident Feedback for Incident

- **Endpoint**: `GET /api/incidents/{incidentId}/feedback`
- **Authorization**: Authenticated user. If citizen, caller must be report owner. Officers and admins have municipal visibility.
- **Response**: `200 OK` with JSON array of `IncidentFeedbackResponse`.

### 3.3 Municipal Officer Reviews Dispute

- **Endpoint**: `POST /api/incidents/{incidentId}/feedback/{feedbackId}/review`
- **Authorization**: `ROLE_OFFICER` or `ROLE_ADMIN`
- **Request Body**:
  ```json
  {
    "status": "ACCEPTED_FOR_REWORK | UNDER_SUPERVISION | DISMISSED",
    "reviewNotes": "Supervisory review instructions and findings (max 2000 chars)"
  }
  ```
- **Responses**:
  - `200 OK`: Review recorded; `reviewed: true`, `reviewedBy: officerName`, `reviewedAt: ISO timestamp`.
  - `400 Bad Request`: Feedback is not a dispute or invalid review status.
  - `403 Forbidden`: Citizen attempted to perform officer dispute review.

---

## 4. State Transitions & Audit Trails

```
[RESOLVED] (Incident resolved by field officer with photo proof)
    │
    ├── Resident Confirms ──> Stays [RESOLVED]
    │                         └── Append audit: RESIDENT_CONFIRMED
    │                         └── Feedback record: status = CONFIRMED
    │
    └── Resident Disputes ──> Transitions to [REPORTED]
                              └── Append audit: RESIDENT_DISPUTED
                              └── Append audit: REOPENED (routed to queue)
                              └── Feedback record: status = PENDING_REVIEW
                              └── Resolution & Evidence: PRESERVED 100%
                                    │
                                    └── Officer Reviews Dispute
                                          └── Append audit: DISPUTE_REVIEWED
                                          └── Feedback record: status = ACCEPTED_FOR_REWORK / UNDER_SUPERVISION / DISMISSED
```

---

## 5. Security & Verification Tests

A dedicated test suite `ResidentFeedbackDisputeSecurityTest.java` was authored covering all 12 security requirements:

| # | Test Scenario | Verified Behavior | Status |
| :--- | :--- | :--- | :--- |
| 1 | `testOriginalReporterCanConfirmResolution` | Authenticated reporter confirms resolved incident; returns HTTP 201; audit log contains `RESIDENT_CONFIRMED`; incident stays `RESOLVED`. | **PASSED** |
| 2 | `testOriginalReporterCanDisputeResolution` | Reporter disputes with explanation; returns HTTP 201; incident reopens to `REPORTED`; audit log contains `RESIDENT_DISPUTED` and `REOPENED`. | **PASSED** |
| 3 | `testAnotherResidentCannotSubmitFeedback` | Different citizen tries to submit feedback on another's incident; rejected with `HTTP 403 Forbidden`. | **PASSED** |
| 4 | `testUnauthenticatedRequestRejected` | No Authorization header; rejected with `HTTP 401 Unauthorized`. | **PASSED** |
| 5 | `testOfficerCannotSubmitResidentFeedback` | Officer account calls resident feedback endpoint; rejected with `HTTP 403 Forbidden`. | **PASSED** |
| 6 | `testFeedbackAgainstUnresolvedIncidentRejected` | Feedback submitted on `REPORTED` or `IN_PROGRESS` incident; rejected with `HTTP 400 Bad Request`. | **PASSED** |
| 7 | `testBlankDisputeExplanationRejected` | Dispute with empty or whitespace explanation; rejected with `HTTP 400 Bad Request`. | **PASSED** |
| 8 | `testOversizedDisputeExplanationRejected` | Dispute with >2000 character string; rejected with `HTTP 400 Bad Request`. | **PASSED** |
| 9 | `testDuplicateFeedbackRejected` | Second feedback submission on the same incident; rejected with `HTTP 409 Conflict`. | **PASSED** |
| 10 | `testDisputePreservesResolutionAndEvidence` | After dispute, `resolutions` and `evidence` tables are checked: rows, content hashes, similarity scores, and officer remarks are completely intact. | **PASSED** |
| 11 | `testAuthorizedOfficerCanReviewDispute` | Officer reviews dispute with status and instructions; returns HTTP 200; feedback marked `reviewed = true`; audit log contains `DISPUTE_REVIEWED`. | **PASSED** |
| 12 | `testCitizenCannotReviewDispute` | Citizen attempts to review a dispute; rejected with `HTTP 403 Forbidden`. | **PASSED** |

### Complete Test Suite Execution Results:
```
[INFO] Results:
[INFO] Tests run: 252, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 02:39 min
```
Zero regressions across all existing authentication, privacy isolation, evidence verification, and Judge Demo test suites.

---

## 6. Frontend Verification

1. **`services/index.js`**: Exported `feedbackService` (`submitFeedback`, `getFeedbackForIncident`, `reviewDispute`).
2. **`IncidentDetail.jsx`**:
   - Integrates `useAuth()` to identify the current session user.
   - Fetches feedback data in parallel via `feedbackService.getFeedbackForIncident(fetchId)`.
   - Passes `feedbackList`, `currentUser`, and refresh handler to `ResolutionEvidenceVerification`.
3. **`ResolutionEvidenceVerification.jsx`**:
   - Renders Section 3: **Resident Resolution Confirmation & Dispute Assessment**.
   - Displays real-time status badges (`RESOLUTION CONFIRMED`, `DISPUTED BY RESIDENT`, `AWAITING RESIDENT FEEDBACK`).
   - For `CITIZEN` on resolved incidents: renders "Confirm Resolution" and "Dispute Resolution" action buttons.
   - Confirmation modal allows optional comments and records satisfied completion.
   - Dispute modal enforces non-empty explanation with live character counter (0–2000 chars) and error handling.
   - For `OFFICER` / `ADMIN`: displays "Review Dispute" modal allowing formal review notes and disposition selection (`ACCEPTED_FOR_REWORK`, `UNDER_SUPERVISION`, `DISMISSED`).
   - Immutable audit trail timeline renders dedicated badges for `RESIDENT_CONFIRMED`, `RESIDENT_DISPUTED`, and `DISPUTE_REVIEWED`.

### Frontend Quality Verification:
- **Lint**: `npm run lint` -> `Found 50 warnings and 0 errors.` (Exit code 0).
- **Production Build**: `npm run build` -> `✓ built in 14.35s` (Exit code 0).

---

## 7. Modified & Created Files Summary

| Component | Path | Description |
| :--- | :--- | :--- |
| **Backend Migration** | `nirvaha-backend/.../db/migration/V3__resident_feedback_and_disputes.sql` | DDL for `incident_feedbacks` table and foreign key relationships. |
| **Backend Enums** | `nirvaha-backend/.../entity/FeedbackType.java` | Enum for `CONFIRMED` and `DISPUTED`. |
| **Backend Enums** | `nirvaha-backend/.../entity/ActionType.java` | Added `RESIDENT_CONFIRMED`, `RESIDENT_DISPUTED`, `DISPUTE_REVIEWED`. |
| **Backend Entity** | `nirvaha-backend/.../entity/IncidentFeedback.java` | JPA entity linking incident, resident, resolution, and officer review. |
| **Backend Repository** | `nirvaha-backend/.../repository/IncidentFeedbackRepository.java` | Data access methods with demo-run filtering. |
| **Backend DTOs** | `nirvaha-backend/.../dto/ResidentFeedbackRequest.java` | Inbound validation DTO for confirmation/dispute. |
| **Backend DTOs** | `nirvaha-backend/.../dto/DisputeReviewRequest.java` | Inbound validation DTO for officer review. |
| **Backend DTOs** | `nirvaha-backend/.../dto/IncidentFeedbackResponse.java` | Outbound response mapping entity data. |
| **Backend Service** | `nirvaha-backend/.../service/ResidentFeedbackService.java` | Core business logic, ownership checks, transition lifecycle, and audit logs. |
| **Backend Controller**| `nirvaha-backend/.../controller/ResidentFeedbackController.java` | REST endpoints for submission, querying, and review. |
| **Backend Security** | `nirvaha-backend/.../config/SecurityConfig.java` | Role authorization rules for feedback and review endpoints. |
| **Backend Exception** | `nirvaha-backend/.../exception/GlobalExceptionHandler.java` | Added handler for `InvalidWorkflowException` (400) and improved logging. |
| **Backend Tests** | `nirvaha-backend/.../ResidentFeedbackDisputeSecurityTest.java` | 12 automated security and lifecycle test cases. |
| **Frontend Services** | `nirvaha-frontend/src/services/index.js` | API client functions for resident feedback and dispute review. |
| **Frontend UI** | `nirvaha-frontend/src/pages/government/IncidentDetail.jsx` | Connected auth context, feedback fetch, and state synchronization. |
| **Frontend UI** | `nirvaha-frontend/src/components/evidence/ResolutionEvidenceVerification.jsx` | Full confirmation/dispute panel, modals, validation, and timeline badges. |
| **Audit Log** | `E:\Nirvaha\PRODUCT_RELEASE_READINESS_AUDIT.md` | Updated release readiness matrix, marking Gaps 3 and 4 as RESOLVED. |

---

## 8. Recommended Next Step: Phase 4

With Gaps 1, 2, 3, 4, and 5 resolved, the primary remaining blocker in the resident journey is **Gap 6: No Notification Pipeline**. 
Currently, residents and officers must manually poll or refresh the web interface to discover status changes, resolution submissions, or disputes. Implementing an in-app and event-driven notification mechanism will close the feedback loop for Puducherry residents.
