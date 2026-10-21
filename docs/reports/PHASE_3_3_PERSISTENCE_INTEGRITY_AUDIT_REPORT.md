# Nirvaha Phase 3.3 — Final Persistence and Lifecycle Integrity Audit Report

**Date**: 2026-10-14  
**Project Root**: `E:\Nirvaha`  
**Backend**: `E:\Nirvaha\nirvaha-backend`  
**Frontend**: `E:\Nirvaha\nirvaha-frontend`  
**Status**: Completed — Verified & Hardened  

---

## 1. Executive Summary

Phase 3.3 conducted a read-first, deep persistence and lifecycle integrity audit of Nirvaha's resident feedback, resolution ownership, transaction concurrency, database constraints, and Flyway schema execution prior to Phase 4.

Key achievements:
- **Enforced Cross-Incident Resolution Isolation**: Fixed potential cross-incident resolution tampering in both feedback submission and dispute review.
- **Predictable Constraint Violation Handling**: Added `DataIntegrityViolationException` mapping in `GlobalExceptionHandler` to translate unique constraint violations into predictable `HTTP 409 Conflict` responses rather than leaking unhandled 500 errors.
- **Verified Transaction & Concurrency Boundaries**: Pessimistic row locking (`SELECT ... FOR UPDATE`) is confirmed to execute within active `@Transactional` boundaries, serializing concurrent requests and preventing race conditions.
- **Honest Flyway Execution Assessment**: Formally audited and documented that Spring Boot does **not** execute Flyway at runtime (Hibernate `ddl-auto=update` is active), and verified SQL migration scripts (`V1`–`V4`) on an isolated disposable database.
- **Automated Test Results**:
  - Focused Security & Lifecycle Suite (`ResidentFeedbackDisputeSecurityTest`): **25/25 tests passed**.
  - Complete Backend Suite: **265/265 tests passed, 0 failures, 0 errors, 0 skipped**.
- **Scope Compliance**: Work remained strictly within `E:\Nirvaha`. `D:\Nirvaha-AI\nirvaha-ai` was untouched. Phase 4 notifications were not initiated.

---

## 2. Files Changed and Technical Rationale

1. `E:\Nirvaha\nirvaha-backend\src\main\java\com\nirvaha\nirvaha_backend\service\ResidentFeedbackService.java`:
   - *Rationale*: Enforced strict resolution-to-incident ownership checks in `submitFeedback` and `reviewDispute`.
   - In `submitFeedback`: Validated that `activeResolution` must belong to the incident being submitted on (`activeResolution.getIncident().getId().equals(incident.getId())`).
   - In `reviewDispute`: Validated that `feedback.getResolution()` must belong to the incident under review. Furthermore, for `DISMISSED` review, verified that `originalResolution` exists, has valid `resolutionEvidence`, and belongs strictly to the reviewed incident.
2. `E:\Nirvaha\nirvaha-backend\src\main\java\com\nirvaha\nirvaha_backend\exception\GlobalExceptionHandler.java`:
   - *Rationale*: Added `@ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)` to translate database unique constraint violations (specifically `uq_incident_feedbacks_resolution`) into clean `HTTP 409 Conflict` responses, preventing internal server error leakage.
3. `E:\Nirvaha\nirvaha-backend\src\test\java\com\nirvaha\nirvaha_backend\ResidentFeedbackDisputeSecurityTest.java`:
   - *Rationale*: Added tests 23, 24, and 25 covering cross-incident resolution tampering rejection, predictable 409 conflict handling, and multithreaded concurrent submission serialization.
4. `E:\Nirvaha\nirvaha-backend\src\main\resources\db\migration\V4__resident_feedback_resolution_unique_index.sql`:
   - *Rationale*: Added forward-only migration ensuring reproducible idempotent application of the partial unique index across any existing installations.

---

## 3. Cross-Incident Resolution Validation

### Verification Points
- **Feedback-to-Resolution Association**:
  In `ResidentFeedbackService.submitFeedback`, the active resolution is fetched specifically for the incident via `resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId)`. An explicit validation check verifies that `activeResolution.getIncident().getId().equals(incident.getId())`.
- **Dispute Review Cross-Incident Tampering**:
  In `reviewDispute`, three levels of ownership isolation are enforced:
  1. `feedback.getIncident().getId().equals(incidentId)`: Validates that the feedback belongs to the incident route parameter.
  2. `feedback.getResolution().getIncident().getId().equals(incidentId)`: Validates that the resolution attached to the feedback belongs to the incident being reviewed.
  3. `originalResolution.getIncident().getId().equals(incidentId)`: For `DISMISSED` disposition, validates that the affirmed resolution record belongs to the incident.
- **Authorization**:
  - Only the authenticated original citizen reporter can submit feedback (`incident.getReporter().getId().equals(currentUser.getId())`).
  - Only users with `ROLE_OFFICER` or `ROLE_ADMIN` can review disputes.
  - Officers cannot submit citizen feedback via API; citizens cannot review disputes.

---

## 4. Transaction and Concurrency Guarantees

### Architectural Guarantees
- **Transaction Scope**: Both `submitFeedback` and `reviewDispute` are marked with `@Transactional(rollbackFor = Exception.class)`.
- **Lock Ordering & Acquisition**:
  - The transaction begins immediately upon method invocation.
  - The pessimistic row write lock is acquired at the very start of the transaction via `incidentRepository.findByIdForUpdate(incidentId)` executing:
    ```sql
    SELECT i FROM Incident i WHERE i.id = :id FOR UPDATE
    ```
  - This serializes all concurrent operations targeting the same incident before feedback queries, resolution checks, or status updates occur.
- **Service vs. Database Agreement**:
  - *Service Layer*: `feedbackRepository.findByResolutionId(activeResolution.getId())` detects and rejects duplicate feedback attempts with `DuplicateResourceException` (`HTTP 409 Conflict`).
  - *Database Layer*: The partial unique index `uq_incident_feedbacks_resolution ON incident_feedbacks(resolution_id) WHERE resolution_id IS NOT NULL` serves as the fail-safe boundary.
  - *Exception Layer*: If concurrent threads bypass the service read due to racing, `DataIntegrityViolationException` is captured by `GlobalExceptionHandler` and converted to `HTTP 409 Conflict`.

### Actual Concurrency Testing
- **Multi-Threaded Test Executed**: Test 25 (`concurrentFeedbackSubmissionsSerializedByRowLock`) in `ResidentFeedbackDisputeSecurityTest` spins up 2 concurrent threads on an `ExecutorService`, synchronized by a `CountDownLatch` for simultaneous execution against `POST /api/incidents/{id}/feedback`.
- **Observed Outcome**:
  - Thread A acquires row lock, inserts feedback, updates incident state, and returns `HTTP 201 Created`.
  - Thread B is blocked on `findByIdForUpdate`. Upon unblocking, it detects the committed feedback and receives `HTTP 409 Conflict`.
  - Database contains exactly 1 feedback record, and incident status remained fully consistent.

---

## 5. Honest Flyway Execution Assessment

### Current State
- **Dependency Inspection**: `flyway-core` is **not** present in `nirvaha-backend/pom.xml`.
- **Application Configuration**: `spring.flyway.enabled` is **not** configured in `application.properties` or `application-dev.properties`.
- **Runtime Execution**: In the running development environment, Spring Boot relies on Hibernate's `spring.jpa.hibernate.ddl-auto=update`. **Spring Boot does not execute Flyway migrations on application startup.**
- **Database Inspection**: `SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'` on `nirvaha_db` confirms `flyway_schema_history` does **not** exist.
- **Safe Development Policy**: The existing development database was **not** silently converted to Flyway, preserving genuine incident records and avoiding schema history tampering.

### Isolated Disposable Migration Testing
To verify that the SQL scripts (`V1`–`V4`) in `src/main/resources/db/migration/` are syntactically valid and reproducible:
1. **Clean-Install Test**:
   - Created a clean disposable PostgreSQL database (`nirvaha_migration_test_db`).
   - Initialized base schema and executed `V3__resident_feedback_and_disputes.sql`.
   - Result: All tables, foreign keys, and indexes created with 0 errors.
2. **Upgrade Path Test**:
   - Executed `V4__resident_feedback_resolution_unique_index.sql`.
   - Result: Index created idempotently (`NOTICE: relation "uq_incident_feedbacks_resolution" already exists, skipping`).
3. **Partial Index Semantics**:
   - Multiple rows with `resolution_id = NULL` allowed (2 inserted successfully).
   - Non-null `resolution_id` allowed (inserted successfully).
   - Duplicate non-null `resolution_id` rejected with `duplicate key value violates unique constraint "uq_incident_feedbacks_resolution"`.
4. **Cleanup**: Disposable database dropped immediately (`DROP DATABASE nirvaha_migration_test_db`). No passwords or credentials leaked.

### Safe Production Migration Strategy
Before deploying to production:
1. Add `org.flywaydb:flyway-core` and `org.flywaydb:flyway-database-postgresql` to `pom.xml`.
2. Set `spring.jpa.hibernate.ddl-auto=validate` for staging and production profiles.
3. For existing databases, baseline the schema using `flyway.baseline-on-migrate=true` and `flyway.baseline-version=0`.

---

## 6. Feedback Lifecycle Invariants & Review Dispositions

| Review Disposition | Incident State Before | Incident State After | Feedback Status | Resolution Status | Audit Action Logged |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`ACCEPTED_FOR_REWORK`** | `REPORTED` | `REPORTED` | `ACCEPTED_FOR_REWORK` | Unaltered | `DISPUTE_REVIEWED` ("Dispute accepted for rework by officer") |
| **`UNDER_SUPERVISION`** | `REPORTED` | `REPORTED` | `UNDER_SUPERVISION` | Unaltered | `DISPUTE_REVIEWED` ("Dispute assigned for supervisory on-site inspection") |
| **`DISMISSED`** | `REPORTED` | `RESOLVED` | `DISMISSED` | Unaltered | `DISPUTE_REVIEWED` ("Dispute dismissed: officer affirmed original resolution") |

### Invariant Rules Enforced
1. **No False Verification**: `DISMISSED` restores incident status to `RESOLVED` by affirming the original resolution, but **never** logs `ActionType.VERIFIED` or changes `Resolution.verificationStatus`.
2. **Resolution & Evidence Requirement**: `DISMISSED` is rejected if the incident does not possess an existing resolution with photographic evidence.
3. **Operational Protection**: Dispute reviews can only be performed while the incident is in `REPORTED` awaiting review. If work has already progressed to `IN_PROGRESS` or `VERIFIED`, the review request is rejected.
4. **Audit Immutability**: All past actions, dispute rationales, officer review notes, and evidence content hashes remain immutable.

---

## 7. Verification Test Counts

### Focused Test Suite: `ResidentFeedbackDisputeSecurityTest`
- **Command**: `mvn test -Dtest=ResidentFeedbackDisputeSecurityTest`
- **Result**: **25 passed, 0 failures, 0 errors, 0 skipped** (Exit code: 0, execution time: 20.1s).

### Complete Backend Test Suite
- **Command**: `mvn test` in `nirvaha-backend`
- **Result**: **265 passed, 0 failures, 0 errors, 0 skipped** (Exit code: 0, execution time: 1m 11s).

### Frontend Verification
- **ESLint**: `npm run lint` in `nirvaha-frontend` $\rightarrow$ 0 errors, 50 warnings (previously verified; no frontend files modified in Phase 3.3).
- **Vite Build**: `npm run build` in `nirvaha-frontend` $\rightarrow$ Built successfully in 6.17s (previously verified; bundle intact).

---

## 8. Remaining Risks and Phase 4 Recommendations

1. **Production Flyway Activation**: Spring Boot is currently running with Hibernate `ddl-auto=update`. When promoting to staging/production, Flyway must be added to `pom.xml` with proper baselining.
2. **Dispute Re-resolution Limit**: There is currently no hard cap on the number of dispute/rework cycles an incident may undergo before mandatory escalation to an executive director.
3. **Phase 4 Readiness**: The feedback and dispute audit trail (`RESOLUTION_CONFIRMED`, `RESOLUTION_DISPUTED`, `DISPUTE_REVIEWED`) is now fully locked down, atomically consistent, and ready to trigger Phase 4 notification events (email/SMS/push).
