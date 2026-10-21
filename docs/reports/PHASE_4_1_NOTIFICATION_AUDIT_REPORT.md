# Nirvaha Phase 4.1 — Notification Reliability and Lifecycle Audit Report

**Audit Date:** October 14, 2026  
**Target Environment:** Local Municipal Instance / Spring Boot 3.5.0 / Java 21 / PostgreSQL 16.15 / React 19 / Vite 8.2.2  
**Root Workspace:** `E:\Nirvaha`  
**Subprojects:** `nirvaha-backend`, `nirvaha-frontend`  
**Preserved External Workspaces:** `D:\Nirvaha-AI\nirvaha-ai` (Untouched)  

---

## 1. Executive Summary & Audit Objectives

Phase 4.1 conducted an exhaustive reliability, concurrency, recipient mapping, and authorization audit on Nirvaha's in-app notification subsystem. The audit verified event deduplication correctness, post-commit transaction durability under process failure conditions, recipient privacy boundaries, and read-state security.

All identified defects were resolved with surgical fixes and proven with automated tests. The entire backend test suite now passes cleanly (**276 tests passed, 0 failures, 0 errors, 0 skipped**), and the frontend builds cleanly (**0 lint errors, 0 build errors**).

---

## 2. Confirmed Defects and Applied Fixes

### Defect 1: Stale Deduplication Key Suppressing Legitimate Rework Transitions
- **Location:** `IncidentWorkflowService.java` (`startWork`, `assignOfficer`, `addAction`)
- **Root Cause:**
  Previously, deduplication keys were constructed using coarse, static strings such as `"incident-" + incident.getId() + "-status-IN_PROGRESS"` and `"incident-" + incident.getId() + "-assigned-" + officerId`.
  When an incident underwent a dispute, was reopened, and a second repair workflow commenced, the officer's second `STARTED` action generated the exact same deduplication key as the first attempt. Consequently, the resident was suppressed from receiving notifications about the recommencement of field operations.
- **Resolution:**
  Refactored action recording to save the `IncidentAction` entity first, acquiring its auto-generated database identity ID (`action.getId()`). The deduplication keys are now uniquely anchored to the specific lifecycle action:
  - Work Started: `"action-" + saved.getId() + "-status-IN_PROGRESS"`
  - Officer Assignment: `"action-" + saved.getId() + "-assigned-" + actingUser.getId()`
  - Resident Assignment Alert: `"action-" + saved.getId() + "-assigned-res-" + actingUser.getId()`
- **Verification:**
  Retrying the exact same HTTP request continues to yield the same action ID and is safely deduplicated. A subsequent legitimate transition generates a new action ID and reliably creates the new notification. Verified in Test 10 of `NotificationSystemSecurityTest`.

### Defect 2: Poisoned Session and Unhandled Concurrency Race on Duplicate Inserts
- **Location:** `NotificationService.java` (`createNotification`)
- **Root Cause:**
  When multiple concurrent threads or rapid network retries attempted to create notifications with identical deduplication keys, both threads could bypass the initial `findByDeduplicationKey` check before either transaction committed. The second thread subsequently violated the database unique constraint `idx_notifications_dedup_key`.
  Because this exception was caught inside an existing Hibernate persistence context, the session became marked as rollback-only; subsequent entity queries within that session threw `org.hibernate.AssertionFailure: null id in entry` or crashed with unhandled `DataIntegrityViolationException`.
- **Resolution:**
  Refactored `NotificationService.createNotification` to execute its insert logic within a dedicated `TransactionTemplate` configured with `PROPAGATION_REQUIRES_NEW` and explicit `saveAndFlush`.
  When a concurrent unique constraint violation occurs at the database boundary, the inner transaction safely rolls back without dirtying the caller's session. The outer catch block cleanly falls back to querying `findByDeduplicationKey` to return the committed notification record.
- **Verification:**
  Test 11 was added to execute 8 concurrent threads attempting simultaneous insertions with the exact same deduplication key. All 8 threads completed successfully without unhandled exceptions, and exactly 1 persistent record was created in the database.

### Defect 3: Non-Cascading Foreign Key in Integration Test Cleanups
- **Location:** `Notification.java` & `NotificationService.java`
- **Root Cause:**
  Integration tests that execute `userRepository.deleteAll()` (such as `ResolutionServiceTest` and `UserServiceTest`) failed with PostgreSQL foreign key constraint violations because `notifications.recipient_id` lacked an explicit cascade-on-delete constraint.
- **Resolution:**
  Annotated `Notification.recipient` with `@org.hibernate.annotations.OnDelete(action = OnDeleteAction.CASCADE)` and added an idempotent runtime database schema patch in `NotificationService.init()` (`ensureCascadingForeignKey`) to ensure existing PostgreSQL tables enforce `ON DELETE CASCADE`.

---

## 3. Event Deduplication Semantics Matrix

| Event Type | Triggering Method | Primary Recipient | Deduplication Key Format | Rework / Repeat Behavior |
| :--- | :--- | :--- | :--- | :--- |
| `INCIDENT_ASSIGNED` | `assignOfficer`, `addAction(ASSIGNED)` | Assigned Officer | `action-{actionId}-assigned-{officerId}` | Idempotent per action; new assignment generates new notification |
| `INCIDENT_ASSIGNED` | `assignOfficer`, `addAction(ASSIGNED)` | Reporting Resident | `action-{actionId}-assigned-res-{officerId}` | Informs resident of investigator; distinct key per assignment |
| `STATUS_CHANGED` | `startWork`, `addAction(STARTED)` | Reporting Resident | `action-{actionId}-status-IN_PROGRESS` | Fresh action ID on rework guarantees new notification |
| `RESOLUTION_SUBMITTED` | `ResolutionService.submitResolution` | Reporting Resident | `incident-{id}-resolution-{resId}` | Re-resolution produces new `resId`, generating new notification |
| `RESOLUTION_CONFIRMED` | `ResidentFeedbackService.submitFeedback` | Responsible Officer | `feedback-{fbId}-confirmed` | Keyed by feedback entity ID |
| `RESOLUTION_DISPUTED` | `ResidentFeedbackService.submitFeedback` | Responsible Officer | `feedback-{fbId}-disputed` | Keyed by dispute feedback entity ID |
| `DISPUTE_REVIEWED` | `ResidentFeedbackService.reviewDispute` | Reporting Resident | `feedback-{fbId}-review-{disposition}` | Keyed by feedback ID and disposition (`rework`, `supervision`, `dismissed`) |

---

## 4. Transaction Durability and Delivery Guarantees Audit

### Current Architecture Traced:
1. Incident/Action/Resolution/Feedback domain transaction begins.
2. Changes to municipal records are staged in the database.
3. `eventPublisher.publishEvent(new IncidentLifecycleNotificationEvent(...))` publishes to Spring's in-memory `ApplicationEventMulticaster`.
4. Domain transaction commits (`COMMIT` issued to PostgreSQL).
5. Spring fires `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
6. `NotificationEventListener` executes `notificationService.createNotification(event)` in an isolated `REQUIRES_NEW` transaction.

### Honest Delivery Guarantees & Limitations:
- **Zero Ghost Notifications on Rollback:** If the parent municipal transaction rolls back (due to database constraints, validation failures, or business exceptions), the in-memory event is discarded. No orphan notification is ever created. Verified in Test 9.
- **Crash Recovery Limitation:** If the JVM process crashes (power failure, host crash, OS SIGKILL, OOM) *after* the domain transaction commits but *before* the notification listener executes or commits its `REQUIRES_NEW` transaction, **the in-memory event is lost**.
- **No Durable Pending Work Queue:** The current implementation stores notifications directly in the `notifications` table upon execution; there is no persistent message broker (RabbitMQ/Kafka) or transactional outbox table holding pending events before processing.
- **Outbox Evaluation for Project Scope:**
  - Introducing a full Transactional Outbox pattern would require an `outbox` table, polling scheduler/Debezium CDC, lease management, and background worker threads.
  - Given Nirvaha's current single-node deployment profile and municipal operational requirements, introducing an Outbox pattern would add unnecessary architectural complexity without proportionate benefit at this phase.
  - **Decision:** Document this operational boundary honestly rather than over-engineering. In-app notifications are derived audit projections; critical incident lifecycle history remains durably committed in `incident_actions`, `incident_feedbacks`, and `resolutions`.

---

## 5. Recipient Mapping & Privacy Audit

1. **Information Isolation:**
   - Resident dispute explanations are recorded in `IncidentFeedback` and delivered exclusively to the assigned/responsible officer. Unrelated residents cannot view or receive dispute messages.
   - Resident feedback notifications are never broadcast to public feeds.
2. **Review Disposition Neutrality:**
   - `ACCEPTED_FOR_REWORK`: "Your dispute for incident #X has been reviewed and accepted for field rework."
   - `UNDER_SUPERVISION`: "Your dispute for incident #X is scheduled for supervisory field inspection."
   - `DISMISSED`: "Your dispute for incident #X was reviewed and dismissed. Original resolution affirmed. Notes: [Reviewer Notes]."
   - Dismissed dispositions do not falsely claim repairs were verified by the citizen; they state clearly that supervisory review affirmed the original resolution based on the officer's rationale.

---

## 6. Read-State Authorization & Security Verification

1. **Principal Derivation:**
   - All notification retrieval and mutation endpoints (`/api/notifications`, `/api/notifications/unread-count`, `/api/notifications/{id}/read`, `/api/notifications/read-all`) extract the recipient ID strictly from Spring Security's authenticated `Principal` (`SecurityContextHolder.getContext().getAuthentication().getName()`).
   - No endpoint accepts or trusts caller-supplied recipient parameters.
2. **Cross-User Modification Shield:**
   - Attempting to mark another user's notification as read throws `AccessDeniedException` (HTTP 403 Forbidden). Verified in Test 5.
3. **Bulk Mark-All-Read Isolation:**
   - `markAllAsReadForCurrentUser` executes an update query strictly scoped to `WHERE recipient_id = :recipientId AND is_read = false`. Verified in Test 6.
4. **Pagination Bounds:**
   - `page` is clamped to `Math.max(0, page)`.
   - `size` is clamped to `Math.min(Math.max(1, size), 100)`.
5. **Unauthenticated Access:**
   - Unauthenticated requests are rejected with HTTP 401 Unauthorized. Verified in Test 1.

---

## 7. Test and Build Verification Results

### Focused Notification Test Suite
```bash
mvn test -Dtest=NotificationSystemSecurityTest
```
- **Results:** Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
- **Execution Time:** 23.78 seconds
- **Exit Code:** 0

### Full Backend Test Suite
```bash
mvn test
```
- **Results:** Tests run: 276, Failures: 0, Errors: 0, Skipped: 0
- **Execution Time:** 1 minute 46 seconds
- **Exit Code:** 0

### Frontend Lint and Build Verification
```bash
npm run lint
```
- **Results:** 0 errors, 52 warnings (in pre-existing components)
- **Exit Code:** 0

```bash
npm run build
```
- **Results:** Built in 1.47 seconds (clean output)
- **Exit Code:** 0

---

## 8. Remaining Operational Risks

1. **JVM Crash Window:**
   - A server restart occurring during the millisecond window between the commit of an incident status change and the execution of the asynchronous event listener will result in a dropped in-app notification.
2. **Long-Term Table Growth:**
   - High municipal volume will accumulate rows in the `notifications` table over years. A TTL cleanup job (e.g. archiving notifications older than 180 days) should be scheduled in future maintenance phases.
