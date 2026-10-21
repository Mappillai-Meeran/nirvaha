# Nirvaha Phase 4 — Reliable In-App Notification System Report

**Date:** October 14, 2026  
**Environment:** Local Development / Spring Boot 3.5.0 / Java 21 / PostgreSQL 16.15 / Vite 8.2.2 / React 19  
**Branch / Target:** `nirvaha-backend` & `nirvaha-frontend` (`E:\Nirvaha`)

---

## 1. Executive Summary

Phase 4 introduces a dedicated, persistent **in-app notification system** for the Nirvaha municipal civic intelligence platform. This system informs residents and municipal officers about critical incident lifecycle events (investigation assignment, field work commencement, resolution submission, resident resolution confirmation, resident dispute escalation, dispute review disposition, and re-resolution) in a tamper-resistant, idempotent, and transaction-safe manner.

Key architectural guarantees achieved:
- **Zero Phantom Notifications:** Event publishing is isolated and executed via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. If the enclosing incident lifecycle transaction rolls back, zero notification records are written.
- **Strict Deduplication & Idempotency:** Every notification event computes a deterministic, unique deduplication key (e.g. `incident-1042-assigned-15177`, `feedback-89-disputed`). Repeated lifecycle calls or retry loops never produce duplicate notifications.
- **Identity Isolation & Secure Authorization:** Notifications can only be retrieved, marked as read, or altered by the authenticated principal matching `recipient_id`. Cross-user viewing and mutation attempts are strictly blocked and audited.
- **Zero External Delivery Claims:** Operates entirely as a persistent, in-app notification hub; no external SMS, WhatsApp, or email delivery is fabricated.
- **Clean Frontend Integration:** Unread notification count badge and responsive notification dropdown drawer added to the official Puducherry civic header without impacting unrelated screens.

---

## 2. Event-to-Recipient Mapping Matrix

| Lifecycle Transition | Domain Event | Recipient | Deduplication Key Format | Title / Message Semantics |
| :--- | :--- | :--- | :--- | :--- |
| **Officer Assignment** | `INCIDENT_ASSIGNED` | **Assigned Officer** | `incident-{id}-assigned-{officerId}` | Informs officer of their assignment with incident ID & title |
| **Officer Assignment** | `INCIDENT_ASSIGNED` | **Reporting Resident** | `incident-{id}-assigned-res-{officerId}` | Informs citizen that a field officer has been assigned to inspect their report |
| **Field Work Started** | `STATUS_CHANGED` | **Reporting Resident** | `incident-{id}-status-IN_PROGRESS` | Informs citizen that field operations have commenced on site |
| **Resolution Submitted** | `RESOLUTION_SUBMITTED` | **Reporting Resident** | `incident-{id}-resolution-{resId}` | Informs citizen that municipal work is complete with resolution evidence ready for review |
| **Resolution Confirmed** | `RESOLUTION_CONFIRMED` | **Responsible Officer** | `feedback-{fbId}-confirmed` | Congratulates officer that resident accepted resolution; case closed |
| **Resolution Disputed** | `RESOLUTION_DISPUTED` | **Responsible Officer** | `feedback-{fbId}-disputed` | Alerts officer that resident disputed repair with detailed citizen explanation |
| **Dispute Disposition** | `DISPUTE_REVIEWED` | **Reporting Resident** | `dispute-review-{fbId}-{disposition}` | Notifies resident of government review decision (`ACCEPTED_FOR_REWORK`, `UNDER_SUPERVISION`, or `DISMISSED`) |

---

## 3. Persistent Notification Model

### Entity Schema (`notifications` table)
- **`id`** (`BIGINT`, Primary Key, Identity): Unique sequential notification ID.
- **`recipient_id`** (`BIGINT`, Foreign Key to `users(id)`, Indexed): Authenticated user who owns this notification.
- **`event_type`** (`VARCHAR(50)`, Not Null): Enum `NotificationEventType` (`INCIDENT_ASSIGNED`, `STATUS_CHANGED`, `RESOLUTION_SUBMITTED`, `RESOLUTION_CONFIRMED`, `RESOLUTION_DISPUTED`, `DISPUTE_REVIEWED`).
- **`title`** (`VARCHAR(255)`, Not Null): Clear, human-readable summary.
- **`message`** (`TEXT`, Not Null): Contextual explanation. Free from JWTs, passwords, hashes, or unnecessary PII.
- **`incident_id`** (`BIGINT`, Nullable, Indexed): ID of related incident for deep-linking.
- **`deduplication_key`** (`VARCHAR(255)`, Unique, Indexed): Deterministic idempotency hash.
- **`is_read`** (`BOOLEAN`, Default `false`, Indexed with recipient): Read state flag.
- **`read_at`** (`TIMESTAMP WITH TIME ZONE`, Nullable): Exact timestamp when user acknowledged notification.
- **`created_at`** (`TIMESTAMP WITH TIME ZONE`, Default `CURRENT_TIMESTAMP`, Indexed with recipient): Chronological ordering.

---

## 4. Transaction Safety and Reliability Architecture

### Post-Commit Event Listener Pattern
```text
[Incident Action / Resolution / Feedback Transaction]
        │
        ├── 1. Persist Incident / Action / Feedback entity
        ├── 2. Publish IncidentLifecycleNotificationEvent to ApplicationEventPublisher
        │
        ├── [Commit Check]
        │       ├── On Rollback ───► Event discarded. ZERO notification persisted.
        │       └── On Commit   ───► Proceed to TransactionalEventListener (phase = AFTER_COMMIT)
        ▼
[NotificationEventListener.handleLifecycleEvent]
        │
        └── NotificationService.createNotification (Propagation.REQUIRES_NEW)
                ├── Check findByDeduplicationKey (Idempotency check)
                ├── If already exists ──► Log dedup skip; return existing record
                └── If new ─────────────► Persist Notification entity & commit
```

- **Propagation Isolation:** The listener invokes `NotificationService.createNotification` under `Propagation.REQUIRES_NEW`. This guarantees that notification insertion is completely isolated and cannot fail or corrupt the already committed parent transaction.
- **Idempotency Guarantee:** If a municipal worker or resident triggers an API retry or re-submits a form, the database unique constraint and pre-insertion deduplication check safely return the existing notification without creating duplicates.

---

## 5. API Endpoints & Security Authorization

All notification endpoints require authentication (`Bearer <JWT>`) and are accessible to `CITIZEN`, `OFFICER`, and `ADMIN`.

| HTTP Method | Path | Required Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/notifications?page=0&size=20` | Authenticated | Paginated list of current user's notifications, ordered `createdAt DESC`. |
| `GET` | `/api/notifications/unread-count` | Authenticated | Total count of unread notifications for current authenticated user. |
| `PATCH` / `PUT` | `/api/notifications/{id}/read` | Authenticated | Marks a specific notification as read. Strict ownership check applied. |
| `PATCH` / `PUT` | `/api/notifications/read-all` | Authenticated | Marks all unread notifications for the current user as read in bulk. |

### Security Authorization Rules
1. **Principal Derivation:** The user's ID is extracted exclusively from the validated Spring Security `Authentication` token (`userRepository.findByEmail(auth.getName())`), completely preventing caller-supplied ID spoofing.
2. **Strict Isolation:** `userCannotMarkAnotherUsersNotificationAsRead` test verifies that any attempt by User B to view or mark User A's notification as read results in an `AccessDeniedException` (HTTP 403) or `ResourceNotFoundException` (HTTP 404), with zero data leakage.

---

## 6. Frontend Integration

1. **API Client (`src/services/apiClient.js` & `src/services/index.js`):**
   - Added HTTP `PATCH` method support.
   - Added `notificationService` with methods: `getNotifications(page, size)`, `getUnreadCount()`, `markAsRead(id)`, `markAllAsRead()`.

2. **Notification Component (`src/components/notifications/NotificationDropdown.jsx`):**
   - **Trigger:** Accessible bell icon button with red badge count (e.g. `1`, `9+`).
   - **Polling:** Automatically fetches unread count every 30 seconds and refreshes on opening.
   - **Dropdown Panel:**
     - Header with title, unread pill, and "Mark all read" button.
     - Scrollable list with distinct visual styling for unread vs read notifications (teal border accent, highlight background, event-specific icons).
     - Individual "Mark as read" action and deep-link click handling navigating to `/citizen/my-reports` or `/command-center/queue`.
     - Loading, empty, and retry error states.
     - Outside click and Escape key listeners for accessibility.

3. **Official Header Integration (`src/components/layout/Header.jsx`):**
   - Positioned alongside `LanguageSwitcher` and role badge when user is logged in.

---

## 7. Changed and Created Files

### Backend
- `src/main/java/com/nirvaha/nirvaha_backend/entity/NotificationEventType.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/entity/Notification.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/event/IncidentLifecycleNotificationEvent.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/repository/NotificationRepository.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/dto/NotificationResponse.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/dto/UnreadCountResponse.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/service/NotificationService.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/service/NotificationEventListener.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/controller/NotificationController.java` *(Created)*
- `src/main/java/com/nirvaha/nirvaha_backend/config/SecurityConfig.java` *(Updated: permitted `/api/notifications/**` for authenticated users)*
- `src/main/java/com/nirvaha/nirvaha_backend/service/IncidentWorkflowService.java` *(Updated: publish events on ASSIGNED and STARTED)*
- `src/main/java/com/nirvaha/nirvaha_backend/service/ResolutionService.java` *(Updated: publish event on RESOLUTION_SUBMITTED)*
- `src/main/java/com/nirvaha/nirvaha_backend/service/ResidentFeedbackService.java` *(Updated: publish events on feedback confirmation, dispute, and review)*
- `src/test/java/com/nirvaha/nirvaha_backend/NotificationSystemSecurityTest.java` *(Created: 9 comprehensive security, transaction, and lifecycle tests)*

### Frontend
- `src/services/apiClient.js` *(Updated: added patch method)*
- `src/services/index.js` *(Updated: exported notificationService)*
- `src/components/notifications/NotificationDropdown.jsx` *(Created: accessible notification bell & drawer)*
- `src/components/layout/Header.jsx` *(Updated: integrated NotificationDropdown)*

---

## 8. Verification & Test Results

- **Full Backend Test Suite (`mvn test`):**
  - **Tests Run:** 274
  - **Failures:** 0
  - **Errors:** 0
  - **Skipped:** 0
  - **Result:** **BUILD SUCCESS** (all 274 integration and unit tests passing)

- **Dedicated Notification Security Suite (`NotificationSystemSecurityTest.java`):**
  - `unauthenticatedRequestCannotAccessNotifications` — **PASSED**
  - `userCanOnlyReadOwnNotifications` — **PASSED**
  - `userCannotMarkAnotherUsersNotificationAsRead` — **PASSED**
  - `markAsReadUpdatesStateAndTimestamp` — **PASSED**
  - `markAllAsReadUpdatesOnlyCurrentUsersUnread` — **PASSED**
  - `unreadCountAccuratelyReflectsReadStateTransitions` — **PASSED**
  - `duplicateEventWithSameDeduplicationKeyIsIdempotent` — **PASSED**
  - `fullIncidentLifecycleGeneratesExpectedNotifications` — **PASSED**
  - `transactionRollbackDoesNotCreateNotification` — **PASSED**

- **Frontend Code Quality & Production Build:**
  - `npm run lint` — **PASSED (0 errors, 52 pre-existing warnings in unrelated files)**
  - `npm run build` — **PASSED (Built in 1.47s, 0 errors, 1943 modules transformed)**

---

## 9. Remaining Operational Notes

1. **Email/SMS Gateway Readiness:** In future phases, when real email (SMTP) or SMS (Twilio/CDAC) credentials are provided, external dispatchers can simply subscribe to `IncidentLifecycleNotificationEvent` alongside the in-app notification listener.
2. **Database Maintenance:** Since `ddl-auto=update` is active, the `notifications` table and indexes are automatically managed by Hibernate. When Flyway migrations are introduced in production deployment, the initial migration script should mirror `Notification.java`.
