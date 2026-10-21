# Nirvaha Phase 3.1 — Resident Feedback Lifecycle Audit Report

**Date**: 2026-10-14  
**Project**: Nirvaha Municipal Service-Request Platform  
**Target Root**: `E:\Nirvaha`  
**Backend**: `E:\Nirvaha\nirvaha-backend`  
**Frontend**: `E:\Nirvaha\nirvaha-frontend`  
**Status**: Completed — Verified & Hardened  

---

## 1. Executive Summary

Phase 3.1 conducted a code-backed, verification-first lifecycle audit of the resident resolution confirmation and dispute workflow. Prior to this audit, basic feedback submission endpoints existed, but several critical lifecycle gaps, state transition anomalies, concurrency edge cases, and personal data exposures were present.

All confirmed defects were remediated with surgical code and schema updates. Comprehensive automated integration and security tests were added and validated against the local PostgreSQL test database.

- **Backend Test Suite**: **258 tests passed, 0 failures, 0 errors, 0 skipped** (including 18 focused feedback/dispute security and lifecycle tests).
- **Frontend Quality**: **0 lint errors** (50 stylistic warnings preserved), **Vite production build succeeded** (`dist/` generated cleanly in 9.38s).
- **Work Boundaries**: Strictly confined to `E:\Nirvaha`. Zero changes made to `D:\Nirvaha-AI\nirvaha-ai`. Phase 4 notifications were not started.

---

## 2. Confirmed Defects vs. Already-Correct Behavior

### A. Already-Correct Behavior (Verified via Code & Tests)
1. **Unauthenticated Access Blocked**:
   - `POST /api/incidents/{id}/feedback` and `POST /api/incidents/{id}/feedback/{feedbackId}/review` reject unauthenticated requests with `401 Unauthorized` or `403 Forbidden`.
2. **Citizen Impersonation Blocked**:
   - Only the original resident who reported the incident (`incident.getReporter().getId().equals(currentUser.getId())`) can submit confirmation or dispute feedback. Other authenticated citizens receive `403 Forbidden` (`IncidentAccessValidator.verifyResidentAccess`).
3. **Officer Impersonation Blocked**:
   - Officers cannot submit resident feedback via `POST /api/incidents/{id}/feedback`. The endpoint explicitly enforces `resident` role checks (`Access denied: Only citizens can submit feedback`).
   - Citizens cannot review disputes via `POST /api/incidents/{id}/feedback/{feedbackId}/review`. The endpoint requires `ROLE_OFFICER` or `ROLE_ADMIN` (`Access denied: Only officers can review disputes`).
4. **Resolution Evidence Preservation**:
   - Submitting a dispute does not modify or delete the original `Resolution` entity, its remarks, similarity scores, or underlying `Evidence` blobs/hashes.

### B. Confirmed Defects Identified & Resolved
1. **Defect 1: Dispute Review Dispositions Had No Operational Effect (Critical)**
   - *Previous State*: `DisputeReviewRequest` did not accept a `disposition` field. The backend hardcoded `feedback.setStatus("REVIEWED")` and unconditionally left the incident in `REPORTED` status without distinguishing between an officer dismissing an invalid dispute vs. accepting it for rework.
   - *Fix*:
     - Added `disposition` to `DisputeReviewRequest` (with `@JsonProperty("status")` alias for backward compatibility).
     - Defined 3 explicit review dispositions:
       - `ACCEPTED_FOR_REWORK`: Dispute accepted as valid. Incident remains in `REPORTED` for field crews to rework. Feedback status set to `ACCEPTED_FOR_REWORK`. Logged as `DISPUTE_REVIEWED`.
       - `UNDER_SUPERVISION`: Dispute requires investigation/supervision. Incident remains in `REPORTED`. Feedback status set to `UNDER_SUPERVISION`. Logged as `DISPUTE_REVIEWED`.
       - `DISMISSED`: Dispute rejected by officer (original work determined sufficient). Incident status restored to `RESOLVED`. Feedback status set to `DISMISSED`. Logged as `DISPUTE_REVIEWED`.
       - Invalid dispositions are rejected immediately with `400 Bad Request`.
2. **Defect 2: Multi-Resolution Lifecycle Blocked Post-Rework (Critical)**
   - *Previous State*: The service prevented duplicate feedback using an incident-wide query `feedbackRepository.findByIncidentId(incident.getId())`. If an incident was disputed, reworked, and resolved a second time, the resident was permanently blocked from submitting feedback on the *new* resolution.
   - *Fix*:
     - Scoped feedback deduplication to the active resolution: `feedbackRepository.findByResolutionId(activeResolution.getId())`.
     - An incident's feedback history remains linked both to the incident and to the specific resolution record. When rework is completed and a new `Resolution` record is created, the citizen can confirm or dispute the new resolution cleanly without destroying past dispute history.
3. **Defect 3: Concurrency Race Condition on Feedback & Dispute Reviews (High)**
   - *Previous State*: Concurrent submissions on the same incident could race past application-level checks, resulting in conflicting feedback states or duplicate records.
   - *Fix*:
     - Implemented pessimistic write locking via `incidentRepository.findByIdForUpdate(id)` (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) in both `submitFeedback` and `reviewDispute` to serialize concurrent requests.
     - Enforced database-level uniqueness per resolution with a partial unique index:
       `CREATE UNIQUE INDEX IF NOT EXISTS uq_incident_feedbacks_resolution ON incident_feedbacks(resolution_id) WHERE resolution_id IS NOT NULL;`
4. **Defect 4: PII Exposure in Feedback Responses (Medium)**
   - *Previous State*: `IncidentFeedbackResponse` included `residentEmail`, leaking private citizen email addresses across feedback queries.
   - *Fix*: Removed `residentEmail` from `IncidentFeedbackResponse`. Kept only anonymized/safe identification (`residentId` and `residentName`), consistent with the incident privacy protections established in Phase 1.
5. **Defect 5: Missing Cross-Incident Feedback Validation (Medium)**
   - *Previous State*: The review endpoint verified `feedback.getIncident().getId().equals(incident.getId())`, but `ResidentFeedbackDisputeSecurityTest` lacked assertion coverage for ID tampering across mismatched incidents.
   - *Fix*: Verified and hardened the cross-incident check to reject requests with `400 Bad Request` (`IllegalArgumentException`).
6. **Defect 6: Frontend Status Display for Dismissed Disputes (Low)**
   - *Previous State*: `ResolutionEvidenceVerification.jsx` did not handle the `DISMISSED` dispute status badge styling.
   - *Fix*: Added `DISMISSED: 'success'` mapping to the badge variant lookup table.

---

## 3. State Transitions and Disposition Semantics

The platform incident status enum supports: `REPORTED`, `VERIFIED`, `IN_PROGRESS`, `RESOLVED`.

| Event / Action | Actor | Pre-condition | Post-condition (Incident) | Post-condition (Feedback) | Audit Action Logged |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Confirm Resolution** | Resident | Incident is `RESOLVED` | `RESOLVED` | `CONFIRMED` | `RESOLUTION_CONFIRMED` |
| **Dispute Resolution** | Resident | Incident is `RESOLVED` | `REPORTED` (Reopened) | `DISPUTED` | `RESOLUTION_DISPUTED` |
| **Review: ACCEPTED_FOR_REWORK** | Officer / Admin | Incident is `REPORTED`, Feedback is `DISPUTED` | `REPORTED` | `ACCEPTED_FOR_REWORK` | `DISPUTE_REVIEWED` |
| **Review: UNDER_SUPERVISION** | Officer / Admin | Incident is `REPORTED`, Feedback is `DISPUTED` | `REPORTED` | `UNDER_SUPERVISION` | `DISPUTE_REVIEWED` |
| **Review: DISMISSED** | Officer / Admin | Incident is `REPORTED`, Feedback is `DISPUTED` | `RESOLVED` (Re-affirmed) | `DISMISSED` | `DISPUTE_REVIEWED` |
| **Second Resolution Submission** | Officer / Admin | Incident is `REPORTED` or `IN_PROGRESS` | `RESOLVED` | New `Resolution` entity created; previous feedback preserved | `STATUS_CHANGE` |
| **Second Feedback Submission** | Resident | Incident is `RESOLVED` on Resolution #2 | `RESOLVED` (if confirmed) / `REPORTED` (if disputed) | New `IncidentFeedback` attached to Resolution #2 | `RESOLUTION_CONFIRMED` / `RESOLUTION_DISPUTED` |

---

## 4. Database Migration & Schema Changes

### Flyway Migration Update: `V3__resident_feedback_and_disputes.sql`
Updated the baseline V3 migration and applied to active PostgreSQL database:
```sql
CREATE TABLE IF NOT EXISTS incident_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    resolution_id BIGINT REFERENCES resolutions(id) ON DELETE SET NULL,
    resident_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    satisfaction_rating INTEGER NOT NULL CHECK (satisfaction_rating BETWEEN 1 AND 5),
    feedback_type VARCHAR(32) NOT NULL,
    comments TEXT,
    dispute_reason VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    officer_notes TEXT,
    reviewed_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_incident ON incident_feedbacks(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_resident ON incident_feedbacks(resident_id);
CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_resolution ON incident_feedbacks(resolution_id);

-- Enforce at most one feedback record per resolution attempt
CREATE UNIQUE INDEX IF NOT EXISTS uq_incident_feedbacks_resolution 
ON incident_feedbacks(resolution_id) 
WHERE resolution_id IS NOT NULL;
```

---

## 5. Changed Files

1. `E:\Nirvaha\nirvaha-backend\src\main\java\com\nirvaha\nirvaha_backend\dto\DisputeReviewRequest.java`
   - Added `disposition` field with validation, getter/setter, and `@JsonProperty("status")` compatibility.
2. `E:\Nirvaha\nirvaha-backend\src\main\java\com\nirvaha\nirvaha_backend\dto\IncidentFeedbackResponse.java`
   - Removed `residentEmail` field to prevent PII leakage.
3. `E:\Nirvaha\nirvaha-backend\src\main\java\com\nirvaha\nirvaha_backend\repository\IncidentRepository.java`
   - Added `@Lock(LockModeType.PESSIMISTIC_WRITE) Optional<Incident> findByIdForUpdate(Long id)` for pessimistic locking.
4. `E:\Nirvaha\nirvaha-backend\src\main\java\com\nirvaha\nirvaha_backend\repository\IncidentFeedbackRepository.java`
   - Added `Optional<IncidentFeedback> findByResolutionId(Long resolutionId)` for per-resolution deduplication.
5. `E:\Nirvaha\nirvaha-backend\src\main\java\com\nirvaha\nirvaha_backend\service\ResidentFeedbackService.java`
   - Switched to pessimistic row locking (`findByIdForUpdate`).
   - Scoped feedback deduplication to `activeResolution.getId()`.
   - Implemented state transitions for `ACCEPTED_FOR_REWORK`, `UNDER_SUPERVISION`, and `DISMISSED`.
   - Enforced validation rejecting unknown dispositions.
6. `E:\Nirvaha\nirvaha-backend\src\main\resources\db\migration\V3__resident_feedback_and_disputes.sql`
   - Added `uq_incident_feedbacks_resolution` partial unique index.
7. `E:\Nirvaha\nirvaha-backend\src\test\java\com\nirvaha\nirvaha_backend\ResidentFeedbackDisputeSecurityTest.java`
   - Expanded to 18 comprehensive integration tests covering all roles, dispositions, multi-resolution cycles, cross-incident isolation, and atomicity.
8. `E:\Nirvaha\nirvaha-frontend\src\components\ResolutionEvidenceVerification.jsx`
   - Added badge styling for `DISMISSED` dispute status.

---

## 6. Verification Results

### A. Focused Security & Lifecycle Tests
**Suite**: `ResidentFeedbackDisputeSecurityTest`  
**Execution Command**: `mvn test -Dtest=ResidentFeedbackDisputeSecurityTest`  
**Result**: **18 passed, 0 failed, 0 errors**

| Test # | Test Method Name | Verification Objective | Result |
| :--- | :--- | :--- | :--- |
| 1 | `residentConfirmsResolution_persistsFeedbackAndAudit` | Happy path confirmation & audit log | PASSED |
| 2 | `residentDisputesResolution_reopensIncidentAndPersistsAudit` | Happy path dispute & incident reopening | PASSED |
| 3 | `officerReviewsDispute_updatesFeedbackAndAudit` | Officer dispute review with disposition | PASSED |
| 4 | `unauthenticatedUser_cannotSubmitFeedback` | Unauthenticated rejection (401/403) | PASSED |
| 5 | `differentCitizen_cannotSubmitFeedbackForAnotherResident` | Reporter ownership enforcement | PASSED |
| 6 | `officer_cannotSubmitResidentFeedback` | Officer cannot act as resident | PASSED |
| 7 | `citizen_cannotReviewDispute` | Citizen cannot act as officer | PASSED |
| 8 | `cannotSubmitFeedbackOnUnresolvedIncident` | Rejects feedback on non-resolved incident | PASSED |
| 9 | `cannotSubmitDuplicateFeedbackForSameResolution` | Resolution-level deduplication | PASSED |
| 10 | `cannotReviewAlreadyReviewedDispute` | Deduplication on dispute review | PASSED |
| 11 | `feedbackResponse_doesNotExposeResidentEmail` | PII leakage protection | PASSED |
| 12 | `originalResolutionEvidence_remainsIntactAfterDispute` | Evidence and hash immutability | PASSED |
| 13 | `officerReview_dismissedDisposition_restoresResolvedStatus` | `DISMISSED` resets status to `RESOLVED` | PASSED |
| 14 | `officerReview_underSupervisionDisposition_keepsIncidentReported` | `UNDER_SUPERVISION` keeps `REPORTED` | PASSED |
| 15 | `officerReview_invalidDisposition_rejectedWithBadRequest` | Invalid disposition validation | PASSED |
| 16 | `disputedIncident_reResolved_canReceiveNewFeedbackOnNewResolution` | Multi-resolution lifecycle support | PASSED |
| 17 | `feedbackReview_mismatchedIncidentAndFeedbackId_rejected` | Cross-incident ID tampering protection | PASSED |
| 18 | `concurrentFeedbackSubmissions_preventDoubleSubmissionViaLocking` | Pessimistic locking serializes requests | PASSED |

### B. Complete Backend Test Suite
**Command**: `mvn test` (in `nirvaha-backend`)  
**Result**:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 258, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 01:17 min
```

### C. Frontend Verification
1. **ESLint**:
   **Command**: `npm run lint` (in `nirvaha-frontend`)  
   **Result**: 0 errors, 50 warnings (existing unused variables / fast-refresh warnings preserved).
2. **Vite Production Build**:
   **Command**: `npm run build` (in `nirvaha-frontend`)  
   **Result**: `✓ built in 9.38s` — all 1,942 modules transformed, CSS/JS assets generated cleanly.

### D. End-to-End & Browser Verification Note
*Automated verification completed using Spring Boot `@SpringBootTest` mock MVC test harness with actual Hibernate JPA PostgreSQL transactions and row locking. Real browser subagent verification was not invoked in this automated pass, avoiding any unverified claims.*

---

## 7. Remaining Considerations & Next Steps

1. **Phase 4 Readiness**: The resident feedback and dispute lifecycle is now mathematically sound, authorization-isolated, and atomic. It provides stable event hooks (`RESOLUTION_CONFIRMED`, `RESOLUTION_DISPUTED`, `DISPUTE_REVIEWED`) ready for Phase 4 email/SMS/push notifications.
2. **Dispute Resolution Limit**: Currently, there is no hard ceiling on how many rework/dispute cycles an incident may undergo before mandatory supervisor escalation. If municipal policy dictates a maximum of 2 disputes, an escalation rule can be integrated cleanly into `ResidentFeedbackService`.
