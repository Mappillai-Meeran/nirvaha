# Nirvaha Phase 4.2 — Notification Database and Lifecycle Hardening Report

**Audit Date:** October 14, 2026  
**Environment:** Local Development / Spring Boot 3.5.0 / Java 21 / PostgreSQL 16.15 / React 19 / Vite 8.2.2  
**Root Workspace:** `E:\Nirvaha`  
**Subprojects:** `nirvaha-backend`, `nirvaha-frontend`  
**Preserved External Workspaces:** `D:\Nirvaha-AI\nirvaha-ai` (Untouched)  

---

## 1. Executive Summary & Objectives

Phase 4.2 conducted an independent hardening audit of the notification subsystem implemented in Phase 4 and refined in Phase 4.1. This phase eliminated dangerous runtime DDL execution on startup, hardened concurrent transaction rollback boundaries, distinguished genuine deduplication collisions from non-duplicate data integrity failures, strictly enforced post-commit event delivery by removing permissive fallback execution, and expanded the automated regression test suite to 13 security/reliability tests.

### Build and Verification Summary
- **Target Notification Security Test Suite:** 13 passed, 0 failures, 0 errors, 0 skipped (13.55s, exit code 0)
- **Full Backend Test Suite:** 278 passed, 0 failures, 0 errors, 0 skipped (1m 16s, exit code 0)
- **Frontend Code Quality & Build:** 0 lint errors (52 pre-existing warnings in unrelated visualizers), build succeeded in 1.78s (exit code 0)
- **Database Safety:** Zero data loss, zero destructive schema changes, zero runtime DDL executed on boot.

---

## 2. Audit of Automatic Foreign-Key Modification

### Previous Implementation (Phase 4.1)
`NotificationService.java` previously contained a `@PostConstruct` hook (`ensureCascadingForeignKey`) executing raw PL/pgSQL via `JdbcTemplate`:
```sql
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'notifications'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'recipient_id'
    ) LOOP
        EXECUTE 'ALTER TABLE notifications DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
    END LOOP;
END $$;

ALTER TABLE notifications
ADD CONSTRAINT fk_notifications_recipient
FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE;
```

### Verified Vulnerabilities and Deficiencies:
1. **Unstable & Hardcoded Constraint Assumption:** Renamed any existing foreign key to the hardcoded name `fk_notifications_recipient`.
2. **Database Platform Assumption:** The code unconditionally executed PostgreSQL-specific PL/pgSQL (`DO $$`, `quote_ident`), making it non-portable.
3. **Elevated Privilege Requirement on App Startup:** Required table-level DDL permissions (`ALTER TABLE`) during normal application startup, violating least-privilege security standards.
4. **Repeated Table Locking:** On every application start, an `ACCESS EXCLUSIVE` lock was requested on the `notifications` table to drop and recreate constraints, creating deadlock risks during concurrent restarts.
5. **Conflict with Hibernate `ddl-auto=update`:** Hibernate attempts to manage schema state; runtime DDL behind Hibernate's back creates metadata desynchronization.

### Changes Implemented in Phase 4.2:
1. **Completely Removed Runtime DDL:** Stripped `ensureCascadingForeignKey()` and the `JdbcTemplate` dependency entirely from `NotificationService.java`.
2. **Entity Metadata Representation:** Verified that `Notification.java` cleanly defines the cascade rule in JPA/Hibernate metadata:
   ```java
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "recipient_id", nullable = false)
   @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
   private User recipient;
   ```
3. **Verified Existing Database Constraint:** Inspected PostgreSQL `pg_constraint` catalog for table `notifications`:
   - Constraint `fk_notifications_recipient` exists with `contype = 'f'` (foreign key) and `confdeltype = 'c'` (`CASCADE`).
4. **Explicit Migration SQL Plan:** Documented separately for DBA review on new staging/production environments (see Section 6).

---

## 3. Concurrency Handling Verification & Error Distinction

### Verified Mechanics of `TransactionTemplate` / `REQUIRES_NEW`:
```java
try {
    return transactionTemplate.execute(status -> {
        ...
        Notification saved = notificationRepository.saveAndFlush(notification);
        return Optional.of(NotificationResponse.fromEntity(saved));
    });
} catch (org.springframework.dao.DataIntegrityViolationException e) {
    if (event.getDeduplicationKey() != null && !event.getDeduplicationKey().isBlank()) {
        Optional<Notification> existing = notificationRepository.findByDeduplicationKey(event.getDeduplicationKey());
        if (existing.isPresent()) {
            log.info("Duplicate notification detected via database unique constraint on deduplication_key: {}", event.getDeduplicationKey());
            return Optional.of(NotificationResponse.fromEntity(existing.get()));
        }
    }
    log.error("Data integrity violation while persisting notification for incident {}: {}", event.getIncidentId(), e.getMessage(), e);
    throw e;
}
```

### Verified Guarantees:
1. **Complete Inner Rollback:** When a collision occurs on `idx_notifications_dedup_key`, `TransactionTemplate` handles the exception by rolling back the inner `REQUIRES_NEW` transaction prior to exiting `transactionTemplate.execute`.
2. **Clean Outer Persistence Context:** The caller's surrounding session is not dirtied or marked rollback-only.
3. **Post-Rollback Retrieval:** `findByDeduplicationKey` executes only after the failed insert's inner transaction is rolled back, querying the committed record inserted by the winning thread.
4. **Non-Duplicate Integrity Failures Rethrown:** If a `DataIntegrityViolationException` is triggered by a check constraint, column length overflow, or non-null violation rather than a duplicate deduplication key, the catch block verifies `existing.isPresent()`. If false, it logs the error and rethrows `DataIntegrityViolationException` rather than masking it as a duplicate (proven in Test 12).
5. **Thread-Safe Result Consistency:** In an 8-thread race condition test, all 8 threads received `Optional.of(NotificationResponse)` with the exact same database entity ID, and exactly 1 persistent record exists in PostgreSQL (proven in Test 11).

---

## 4. Lifecycle Deduplication & Transaction Guarantees

### Action Persistence Precedence
Verified all event-producing methods in `IncidentWorkflowService`, `ResolutionService`, and `ResidentFeedbackService`:
- `assignOfficer`: Action is persisted first via `recordAction` before constructing key `"action-" + action.getId() + "-assigned-" + actingUser.getId()`.
- `startWork`: Action is persisted first via `recordAction` before constructing key `"action-" + action.getId() + "-status-IN_PROGRESS"`.
- `addAction`: Entity is persisted first (`incidentActionRepository.save(action)`) before constructing key `"action-" + saved.getId() + "-..."`.
- `submitResolution`: Entity is persisted first (`resolutionRepository.save(resolution)`) before constructing key `"incident-" + incident.getId() + "-resolution-" + saved.getId()`.
- `submitFeedback`: Feedback is persisted first (`feedbackRepository.save(feedback)`) before constructing key `"feedback-" + saved.getId() + "-..."`.
- `reviewDispute`: Feedback review update is persisted first before constructing key `"feedback-" + saved.getId() + "-review-" + disposition`.

### Strict AFTER_COMMIT Enforcement
In `NotificationEventListener.java`:
```java
// BEFORE (Phase 4.1):
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)

// AFTER (Phase 4.2 Hardened):
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```
- **Rationale:** `fallbackExecution = true` allowed events published outside of an active transaction to fire immediately. In Nirvaha's municipal domain, all state transitions must be transactional. Removing `fallbackExecution = true` guarantees that events are only processed after a real database transaction successfully commits.
- **Verification:** Test 13 publishes an event without an active transaction and verifies that the listener does not fire (`notificationRepository.findByDeduplicationKey(...)` remains empty).

---

## 5. Automated Verification Results

### 1. Focused Notification Security & Concurrency Test Suite
```bash
mvn test -Dtest=NotificationSystemSecurityTest
```
- **Total Tests:** 13
- **Passed:** 13
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Time:** 13.55 seconds
- **Exit Code:** 0

#### Test Inventory:
1. `unauthenticatedRequestRejected`: Rejects unauthenticated requests with HTTP 401.
2. `userRetrievesOnlyTheirOwnNotifications`: Verifies isolation between users.
3. `unreadCountReturnsAccurateCount`: Verifies unread count accuracy.
4. `markAsReadUpdatesStatusAndTimestamp`: Verifies read state and timestamp update.
5. `cannotMarkAnotherUsersNotificationAsRead`: Cross-user read modification returns HTTP 403.
6. `markAllAsReadUpdatesAllForCurrentUser`: Bulk mark-all-read is strictly scoped to current user.
7. `duplicateEventKeyDoesNotCreateDuplicates`: Idempotent handling for repeated logical events.
8. `fullIncidentLifecycleGeneratesExpectedNotifications`: End-to-end workflow (assignment, work, dispute, review).
9. `transactionRollbackDoesNotCreateNotification`: Rollback of parent transaction creates zero notifications.
10. `reopenedIncidentReworkGeneratesLegitimateNotifications`: Reopened incident rework emits legitimate notifications without stale suppression.
11. `concurrentDuplicateNotificationInsertsHandledPredictably`: 8-thread concurrent race condition produces 1 record and consistent response IDs.
12. `unrelatedDataIntegrityViolationIsNotMaskedAsDuplicate`: Non-null database constraint violations are rethrown.
13. `nonTransactionalEventPublicationDoesNotExecuteListener`: Verifies `fallbackExecution = false` prevents execution without an active transaction.

### 2. Full Backend Test Suite
```bash
mvn test
```
- **Total Tests:** 278
- **Passed:** 278
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Execution Time:** 1 minute 16 seconds
- **Exit Code:** 0

### 3. Frontend Verification
```bash
npm run lint
```
- **Results:** 0 errors (52 warnings in unrelated chart components)
- **Exit Code:** 0

```bash
npm run build
```
- **Results:** Built in 1.78 seconds (dist built cleanly)
- **Exit Code:** 0

---

## 6. Schema Migration Plan (For DBA Approval)

Because Flyway is not currently active and Hibernate `ddl-auto=update` is used in development, the foreign key cascading rule is represented in entity metadata (`@OnDelete(action = OnDeleteAction.CASCADE)`).

For new environments or production promotion where tables are managed via migration scripts, the following idempotent SQL should be executed by a database administrator:

```sql
-- Schema Migration: Enforce ON DELETE CASCADE for notifications.recipient_id
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_name = 'notifications'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'recipient_id'
    ) LOOP
        EXECUTE 'ALTER TABLE notifications DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
    END LOOP;
END $$;

ALTER TABLE notifications
ADD CONSTRAINT fk_notifications_recipient
FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE;
```

---

## 7. Changed Files Inventory

1. `nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/service/NotificationService.java`
   - Removed `JdbcTemplate` dependency and `ensureCascadingForeignKey()` `@PostConstruct` logic.
   - Configured `transactionTemplate` with `PROPAGATION_REQUIRES_NEW` in constructor.
   - Hardened `catch (DataIntegrityViolationException)` to verify `existing.isPresent()` before claiming duplicate status, rethrowing unrelated violations.
2. `nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/service/NotificationEventListener.java`
   - Removed `fallbackExecution = true` from `@TransactionalEventListener` to enforce strict `AFTER_COMMIT` transaction guarantee.
3. `nirvaha-backend/src/test/java/com/nirvaha/nirvaha_backend/NotificationSystemSecurityTest.java`
   - Injected `ApplicationEventPublisher`.
   - Enhanced Test 11 assertions (validating consistent notification ID and exact entity attributes across all 8 threads).
   - Added Test 12 (verifying non-duplicate data integrity exceptions are rethrown).
   - Added Test 13 (verifying non-transactional event publications are skipped).
4. `PHASE_4_2_NOTIFICATION_HARDENING_REPORT.md`
   - Audit report and DBA schema migration documentation.

---

## 8. Remaining Limitations and Operational Notes

1. **JVM Crash Window:** The architecture retains the documented boundary: in-memory Spring event listeners execute post-commit. A crash occurring in the millisecond window between PostgreSQL transaction commit and listener execution can lose the notification. A transactional outbox table was evaluated and deferred as unnecessary architectural complexity for the single-node municipal deployment.
2. **Notification Table Retention:** A future maintenance task should establish an automated pruning routine for read notifications older than 180 days.
