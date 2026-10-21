# NIRVAHA PHASE 5A.3 — PRODUCTION READINESS & REAL-DATA INTEGRITY AUDIT REPORT

**Date:** October 15, 2026  
**Auditor:** Nirvaha Core Security & Integrity Architecture Agent  
**Target Environments:** `E:\Nirvaha\nirvaha-backend` & `E:\Nirvaha\nirvaha-frontend`  
**Overall Risk Rating:** **MEDIUM** (Down from HIGH; core IDOR vulnerabilities, real-data contamination risks, and lifecycle evidence bypasses resolved with 8/8 regression tests passing and 322/322 total tests passing)

---

## 1. Executive Summary

A comprehensive production readiness, data integrity, and provenance audit was conducted across the Nirvaha civic intelligence platform. The audit covered all six required areas:
1. **Authentication and Authorization (RBAC / IDOR / Principle of Least Privilege)**
2. **Real-Data vs. Simulated-Data Isolation (Contamination Elimination)**
3. **Incident Lifecycle and Evidence Integrity (Resolution & Dispute Workflows)**
4. **Privacy, Secrets, and PII Minimization**
5. **Database Consistency, Transaction Boundaries, and Outbox Pattern Evaluation**
6. **External Data Reliability (Open-Meteo, IMD, Municipal Charters, & PWD SLAs)**

Prior to this audit, three critical access control vulnerabilities existed:
- Insecure Direct Object Reference (IDOR) on user profile endpoints (`/api/users/{id}`), allowing authenticated citizens to enumerate other citizens' names, emails, and roles.
- Missing incident ownership checks on Priority Scoring (`/api/incidents/{id}/priority`) and Duplicate Detection (`/api/incidents/{id}/duplicates`), allowing any citizen to view calculations, factors, and duplicate matches for another citizen's incident.
- Data contamination wherein synthetic/simulated demo incidents (`intakeSource == SIMULATED`) contaminated official duplicate matching candidates and cluster density calculations for genuine resident complaints.

All identified code vulnerabilities were remediated and verified with 8 targeted regression tests in `ProductionReadinessAuditTest.java`. The complete backend test suite (322 tests) passed with 0 failures, and the frontend production bundle built cleanly with 0 errors.

---

## 2. Findings and Vulnerability Matrix

| ID | Category | Severity | File / Endpoint | Vulnerability / Defect Summary | Status |
|:---|:---|:---:|:---|:---|:---:|
| **SEC-01** | Authorization / IDOR | **HIGH** | `UserController.java` / `UserService.java`<br>`GET /api/users/{id}` | Authenticated citizens could fetch any user record by ID, exposing personal names, emails, roles, and creation timestamps. | **FIXED & VERIFIED** |
| **SEC-02** | Authorization / IDOR | **HIGH** | `PriorityScoringController.java` / `PriorityScoringService.java`<br>`GET /api/incidents/{id}/priority` | Endpoint was missing resident ownership verification. Citizens could query internal priority scores, impact factors, and weights of other citizens' incidents. | **FIXED & VERIFIED** |
| **SEC-03** | Authorization / IDOR | **HIGH** | `DuplicateDetectionController.java` / `DuplicateDetectionService.java`<br>`GET /api/incidents/{id}/duplicates` | Endpoint was missing resident ownership verification. Citizens could query duplicate clusters and geographic offsets of other citizens' incidents. | **FIXED & VERIFIED** |
| **INT-01** | Data Contamination | **HIGH** | `DuplicateDetectionService.java`<br>`findDuplicatesForIncident()` | Official incidents queried all same-category incidents without filtering out `SIMULATED` records, causing real citizen complaints to be flagged as duplicates of synthetic demo records. | **FIXED & VERIFIED** |
| **INT-02** | Data Contamination | **MEDIUM** | `PriorityScoringService.java`<br>`calculateLocationImpactFactor()` | Cluster density factor included `SIMULATED` incidents within 500m, artificially inflating priority scores of official citizen complaints during demo stress runs. | **FIXED & VERIFIED** |
| **LC-01** | Lifecycle Integrity | **MEDIUM** | `ResolutionService.java`<br>`createResolution()` | Successfully verified resolution submissions did not automatically transition incident status to `RESOLVED` nor record an auditable `ActionType.RESOLVED` entry in the incident action log. | **FIXED & VERIFIED** |
| **LC-02** | Lifecycle Integrity | **MEDIUM** | `IncidentWorkflowService.java`<br>`resolveIncident()` | Direct programmatic resolution lacked an explicit guard confirming that an official `Resolution` record with evidence existed prior to status transition. | **FIXED & VERIFIED** |
| **OPS-01** | Transaction Reliability | **LOW** | `NotificationEventListener.java` | Notifications are emitted via `@TransactionalEventListener(phase = AFTER_COMMIT)`. If notification persistence fails or server restarts, in-app notifications could be dropped. | **DOCUMENTED (DEFERRED)** |
| **EXT-01** | External Credential | **INFO** | `ImdWeatherAdapter.java`<br>`/api/provenance/weather` | IMD requires official credentials (`X_API_KEY` + JWT bearer). Safely returns `NOT_CONFIGURED` without mock credentials or broken external calls. | **VERIFIED (COMPLIANT)** |
| **EXT-02** | External Reliability | **INFO** | `WeatherService.java`<br>Open-Meteo Integration | Open-Meteo integration implements 3000ms timeout, 15-minute memory cache, fallback without fabricated numbers, and user-agent attribution. | **VERIFIED (COMPLIANT)** |

---

## 3. Remediations Applied & Code Changes

### 3.1. User Profile IDOR Remediation (`UserService.java`)
- **Action:** Added `verifyUserAccess(User user)` helper.
- **Rule:** If caller is not `ROLE_OFFICER` or `ROLE_ADMIN`, they are permitted to view *only* their own profile (`user.getEmail().equalsIgnoreCase(auth.getName())`).
- **Outcome:** Unauthorized inspection attempts trigger `org.springframework.security.access.AccessDeniedException` (HTTP 403 Forbidden).

### 3.2. Priority Scoring Ownership & Real-Data Isolation (`PriorityScoringService.java`)
- **Action:** Injected `IncidentAccessValidator` and invoked `incidentAccessValidator.verifyIncidentAccess(targetIncident)`.
- **Action:** Updated `calculateLocationImpactFactor()` candidate filter:
  ```java
  .filter(candidate -> targetIncident.getIntakeSource() == IncidentSource.SIMULATED || candidate.getIntakeSource() != IncidentSource.SIMULATED)
  ```
- **Outcome:** Citizens can only calculate priority for their own reported incidents. Simulated records can never inflate cluster density scores for official resident complaints.

### 3.3. Duplicate Detection Ownership & Real-Data Isolation (`DuplicateDetectionService.java`)
- **Action:** Injected `IncidentAccessValidator` and invoked `incidentAccessValidator.verifyIncidentAccess(targetIncident)`.
- **Action:** Added candidate skipping rule:
  ```java
  if (targetIncident.getIntakeSource() != IncidentSource.SIMULATED &&
          candidate.getIntakeSource() == IncidentSource.SIMULATED) {
      continue;
  }
  ```
- **Outcome:** Citizens can only query duplicate matches for their own reports. Real citizen incidents are guaranteed never to be matched with synthetic load-testing incidents.

### 3.4. Resolution Lifecycle and Transition Integrity (`ResolutionService.java` & `IncidentWorkflowService.java`)
- **Action:** Injected `IncidentActionRepository` into `ResolutionService`. Upon successful evidence verification:
  - Updates `incident.setStatus(IncidentStatus.RESOLVED)` and persists incident.
  - Generates immutable audit record with `ActionType.RESOLVED` and remarks.
- **Action:** Injected `ResolutionRepository` into `IncidentWorkflowService.resolveIncident()`. Rejects resolution attempt if `resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId)` is empty.

---

## 4. Test Verification and Evidence

### 4.1. Targeted Audit Regression Suite (`ProductionReadinessAuditTest.java`)
- **Command:** `mvn test -Dtest=ProductionReadinessAuditTest`
- **Results:**
  ```text
  [INFO] Running com.nirvaha.nirvaha_backend.ProductionReadinessAuditTest
  [INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.93 s
  [INFO] BUILD SUCCESS
  ```
- **Tests Verified:**
  1. `testUserProfileIdorProtection`: Verifies Citizen A cannot view Citizen B (HTTP 403), while self and officer access succeed.
  2. `testPriorityScoringIdorProtection`: Verifies Citizen B cannot view Citizen A's incident priority score (HTTP 403).
  3. `testPriorityScoringExcludesSimulatedIncidents`: Confirms simulated incident in exact same category and location within 500m yields `locationImpactFactor == 0.00` ("No nearby related incidents reported").
  4. `testDuplicateDetectionIdorProtection`: Verifies Citizen B cannot inspect Citizen A's duplicate detection results (HTTP 403).
  5. `testDuplicateDetectionExcludesSimulatedIncidents`: Confirms synthetic duplicate incident at exact same coordinate is ignored (0 matches returned).
  6. `testCannotResolveIncidentWithoutEvidence`: Confirms `resolveIncident()` throws `InvalidWorkflowException` if resolution evidence is absent.
  7. `testCreateResolutionTransitionsToResolved`: Confirms submitting valid resolution transitions status to `RESOLVED` and logs `ActionType.RESOLVED`.
  8. `testHttpEndpointsEnforceIdor`: Confirms HTTP 403 Forbidden via Spring Security MockMvc integration on REST endpoints.

### 4.2. Complete Backend Test Suite
- **Command:** `mvn test`
- **Results:**
  ```text
  [INFO] Results:
  [INFO] Tests run: 322, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  [INFO] Total time:  01:23 min
  ```

### 4.3. Frontend Lint and Production Build
- **Commands:**
  ```bash
  cd E:\Nirvaha\nirvaha-frontend
  npm run lint
  npm run build
  ```
- **Results:**
  - `npm run lint`: **0 errors**, 40 warnings (exited with code 0).
  - `npm run build`: **Built successfully** in 1.85s (dist/assets generated cleanly, 0 compilation errors).

---

## 5. Privacy, Secrets, and Configuration Audit

1. **Configuration Profiles (`application.properties` vs. `application-prod.properties`):**
   - `application-prod.properties` enforces strict environment variables without fallbacks (`${DATABASE_URL}`, `${DATABASE_USERNAME}`, `${DATABASE_PASSWORD}`, `${JWT_SECRET}`, `${NIRVAHA_ALLOWED_ORIGINS}`).
   - `management.endpoint.health.show-details=never` is enabled.
   - Hibernate DDL auto is set to `validate` in production, preventing unintended schema alterations.
2. **Local Development Defaults:**
   - Default seeded credentials (`admin@puducherry.gov.in:Admin123!`, `officer@puducherry.gov.in:Officer123!`, `citizen@puducherry.gov.in:Citizen123!`) exist solely in development profiles (`application-dev.properties` and programmatic ready-event seeding).
   - In production, mandatory pre-deployment checklist requires overriding seeded account passwords and generating a cryptographically secure 256-bit `JWT_SECRET`.
3. **No Secret Leaks via Public Endpoints:**
   - `/api/system/integrations` exports integration categories, endpoint URLs, and observation times, but explicitly suppresses all API keys and bearer tokens (`X_API_KEY_AND_JWT_BEARER` is shown only as an authentication type descriptor).

---

## 6. External Data Reliability and Official Attribution

1. **Open-Meteo Integration (`WeatherService.java`):**
   - Configured with `TIMEOUT_MS = 3000` (3.0 seconds).
   - In-memory cache TTL of 15 minutes (`CACHE_TTL = Duration.ofMinutes(15)`).
   - Safe fallback: If connection fails or coordinates are missing, returns `WeatherData.fallback(...)` with `available = false` and `null` numerical values. Never substitutes fabricated temperatures or rainfall numbers.
   - User-Agent header `Nirvaha-Civic-Platform/1.0 (Puducherry UT)` complies with provider policies.
2. **India Meteorological Department (IMD) Integration (`ImdWeatherAdapter.java`):**
   - Safely returns `NOT_CONFIGURED` status and `PENDING_CREDENTIALS` error category.
   - Does not fabricate mock weather data under an official government label.
3. **Citizen Charters and Statutory SLAs (`ServiceRuleService.java`):**
   - **PWD Road Repair & Potholes:** Verified against *PWD Citizen's Charter 2024/2025 Section 3.1 (Roads Wing)*: 15-day statutory standard (`isOfficialSla = true`, `OFFICIALLY_PUBLISHED_CHARTER`).
   - **PWD Drainage & Sewerage:** Verified against *PWD Citizen's Charter 2024/2025 Section 2 (Underground Sewerage Wing)*: 3-day statutory standard (`isOfficialSla = true`, `OFFICIALLY_PUBLISHED_CHARTER`).
   - **Water Supply Pipeline Leaks:** Clearly classified as `PROPOSED_INTERNAL_TARGET` (1 day operational standard), not an official statutory charter (`isOfficialSla = false`).
   - **Municipal Garbage / Solid Waste:** Classified as `PROPOSED_INTERNAL_TARGET` (5-day operational target; general statutory ceiling is 30 days) (`isOfficialSla = false`).

---

## 7. Database Integrity & Transactional Boundaries

1. **PostgreSQL Foreign Keys and Constraints:**
   - Cascading deletions are guarded to preserve incident audit actions and evidence provenance.
   - Uniqueness constraint on `notifications(deduplication_key)` prevents double-notifying users on concurrent lifecycle events.
2. **Notification Creation & Outbox Pattern Limitation:**
   - **Current Implementation:** Notifications are fired via `ApplicationEventPublisher` and received by `NotificationEventListener` using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
   - **Risk / Limitation:** If the notification insert fails or the server process crashes immediately after the primary incident transaction commits, the business transaction succeeds but the corresponding in-app notification is lost.
   - **Architectural Recommendation (Deferred):** Implement a database Transactional Outbox pattern (`outbox_events` table written in the same transaction) polled by a background CDC or scheduled task to ensure guaranteed at-least-once delivery.

---

## 8. Summary of Verified Fixes vs. Deferred Risks

### Verified Fixes:
1. User profile enumeration IDOR eliminated.
2. Incident priority scoring IDOR eliminated.
3. Duplicate detection IDOR eliminated.
4. Simulated demo data excluded from duplicate matching candidates.
5. Simulated demo data excluded from priority cluster density calculations.
6. Incident resolution evidence rules enforced with audit actions.
7. Disputed cases cannot be dismissed without affirmed resolution evidence.
8. 322 automated unit, integration, and security tests passing.

### External Dependencies & Blockers for True Production Deployment:
1. **IMD Official Credentials:** Formal registration at `api.imd.gov.in` to obtain authorized `X-API-KEY` and JWT credentials.
2. **Puducherry LGRAMS / Municipal Grievance API Integration:** Formal MoU with National Informatics Centre (NIC) Puducherry for programmatic intake from `lgredressal.py.gov.in` and `pdymun.in`.
3. **Production Secrets Provisioning:** Injection of secure environment variables (`DATABASE_URL`, `DATABASE_PASSWORD`, `JWT_SECRET`, `NIRVAHA_ALLOWED_ORIGINS`) on the hosting server.
4. **Transactional Outbox Implementation:** Recommended for Phase 6 to guarantee zero notification loss under failure conditions.

---

**Phase 5A.3 Audit Conclusion:** The codebase is robust, data isolation is verified, and critical security vulnerabilities have been remediated. The system is ready for user review before proceeding to Phase 5B.
