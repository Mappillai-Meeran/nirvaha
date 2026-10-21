# NIRVAHA — PHASE 5A.2: OFFICER & ADMIN DASHBOARD EMPTY-STATE INVESTIGATION REPORT

**Date:** 2026-10-15  
**Scope:** `E:\Nirvaha` (Officer & Admin Dashboard Empty-State Root Cause Analysis & Targeted Resolution)  
**Status:** Root Cause Identified, Targeted Fix Applied, 100% Verified (314/314 Backend Tests Pass, 8/8 Regression Tests Pass, Frontend Production Build Clean)

---

## 1. Reproduction Steps & Observed Behavior

### 1.1 Role Authentication & Endpoint Access Matrix

To reproduce the issue, we executed end-to-end HTTP requests against the backend using live credentials for each role:
- **Citizen:** `citizen@nirvaha.gov`
- **Officer:** `officer@nirvaha.gov`
- **Admin:** `admin@nirvaha.gov`

| Endpoint | HTTP Method | Citizen Status | Officer Status | Admin Status | Role Authorization / Notes |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `/api/auth/login` | `POST` | 200 OK | 200 OK | 200 OK | Valid JWT returned with `role` claim |
| `/api/incidents/my-reports` | `GET` | 200 OK | 403 Forbidden | 403 Forbidden | Citizen privacy boundary enforced; officers/admins cannot impersonate citizen reports |
| `/api/incidents` | `GET` | 403 Forbidden | 200 OK / 500* | 200 OK / 500* | Citizen blocked. Officer & Admin allowed (*500 prior to fix due to read-only transaction crash) |
| `/api/action-center/queue` | `GET` | 403 Forbidden | 200 OK | 200 OK | Citizen blocked. Officer & Admin return queue items |
| `/api/integrity/open` | `GET` | 403 Forbidden | 200 OK | 200 OK | Citizen blocked. Officer & Admin return open audit issues |
| `/api/risk/wards` | `GET` | 403 Forbidden | 200 OK | 200 OK | Citizen blocked. Officer & Admin return ward risk maps |
| `/api/intelligence/summary` | `GET` | 403 Forbidden | 200 OK | 200 OK | Citizen blocked. Officer & Admin return intelligence summary |

### 1.2 Dashboard Architecture
- Both **Officer** and **Admin** navigate to `/command-center`, powered by `nirvaha-frontend/src/pages/government/CommandCenter.jsx`.
- Both roles query the same core endpoints: `/api/incidents`, `/api/risk/wards`, `/api/integrity/open`, and `/api/intelligence/summary`.

---

## 2. Root Cause Analysis Supported by Code, API, and DB Evidence

Four distinct root causes combined to cause the empty dashboard states during manual and automated testing:

### Root Cause 1: Enum Disconnect in Frontend Metrics and Status Filtering
- **Backend Model (`IncidentStatus.java`):**
  Only 4 statuses exist in the system:
  ```java
  public enum IncidentStatus {
      REPORTED,
      VERIFIED,
      IN_PROGRESS,
      RESOLVED
  }
  ```
- **Frontend Code (`CommandCenter.jsx` lines 102–104 prior to fix):**
  ```javascript
  const activeCases = incidents.filter(i => 
    i.status === 'OPEN' || i.status === 'IN_PROGRESS' || i.status === 'ASSIGNED'
  ).length;
  ```
  And in the status filter dropdown:
  ```javascript
  <option value="ASSIGNED">Assigned</option>
  ```
- **Impact:**
  - Any newly submitted or verified complaint is in `REPORTED` or `VERIFIED` status. Neither of these matched `OPEN` or `ASSIGNED`.
  - As a result, the KPI card "Active Cases" computed to **0**, and selecting "Assigned" produced an empty list.

### Root Cause 2: Read-Only Transaction Failure on Un-tracked Incidents
- **Backend Code (`IncidentService.java` & `ServiceRuleService.java` prior to fix):**
  `IncidentService.getAllIncidents()` and `getIncidentById()` are marked `@Transactional(readOnly = true)`.
  When building each `IncidentResponse`, `mapToEnrichedResponse()` called:
  ```java
  IncidentServiceTracking tracking = serviceRuleService.evaluateServiceTracking(incident);
  ```
  Inside `ServiceRuleService.evaluateServiceTracking()`:
  ```java
  if (!existingOpt.isPresent()) {
      IncidentServiceTracking t = new IncidentServiceTracking();
      t.setIncident(incident);
      ...
      return trackingRepository.save(tracking); // ATTEMPTS INSERT!
  }
  ```
- **Evidence:**
  When querying an incident that had not yet had its tracking persisted (e.g. test incidents, legacy entries, or direct repo saves), Hibernate executed an `INSERT INTO incident_service_tracking` inside a read-only transaction.
  Postgres immediately rejected the query with:
  ```
  org.postgresql.util.PSQLException: ERROR: cannot execute INSERT in a read-only transaction
  ```
- **Impact:**
  Instead of rendering the dashboard, `GET /api/incidents` threw an HTTP 500 Internal Server Error whenever un-tracked or newly created incidents were fetched.

### Root Cause 3: Strict Simulated-Data Isolation Without Frontend Opt-In
- **Backend Architecture (`IncidentRepository.java`):**
  ```sql
  SELECT i FROM Incident i WHERE (i.intakeSource IS NULL OR i.intakeSource <> 'SIMULATED')
  ```
  `IncidentRepository.findOfficialIncidents()` strictly isolates simulated records (`intakeSource = SIMULATED`) from official citizen submissions by default.
- **Frontend Defect:**
  When testers used the "Judge Demo Mode" modal to trigger flood/cyclone surge scenarios, records were written as `SIMULATED`. However, `nirvaha-frontend/src/services/index.js` never passed `includeSimulated=true` to `/api/incidents` or `/api/action-center/queue`.
- **Impact:**
  In a fresh database with 0 resident submissions, manual testers triggered demo scenarios but the dashboard remained completely empty because the frontend never requested simulated records.

### Root Cause 4: Misleading Filter-Reset Empty State
- **Frontend Code (`CommandCenter.jsx` & `PriorityQueue.jsx` prior to fix):**
  When `incidents.length === 0`, the UI unconditionally displayed:
  `"No open incidents match your search or filter criteria. Reset all filters"`
- **Impact:**
  Even when no records existed in the database, the UI falsely claimed active filters were hiding incidents, confusing operators and manual testers into believing data was suppressed by UI filters.

---

## 3. Affected Roles and Endpoints

| Role | Impacted Routes / Views | Endpoints Affected | Failure Mode |
| :--- | :--- | :--- | :--- |
| **OFFICER** | `/command-center`, `/priority-queue` | `GET /api/incidents`, `GET /api/action-center/queue` | HTTP 500 (read-only transaction on un-tracked items), active cases KPI = 0, demo scenarios invisible, false "reset filter" empty state |
| **ADMIN** | `/command-center`, `/priority-queue` | `GET /api/incidents`, `GET /api/action-center/queue` | Same as Officer (shares government command center view) |
| **CITIZEN** | `/my-reports` | `GET /api/incidents/my-reports` | Unaffected by government view defects; correctly returns only own reports. Read-only transaction fix prevents potential 500 when viewing own report |

---

## 4. Files Changed and Technical Rationale

### 4.1 Backend Changes
1. **`nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/service/ServiceRuleService.java`**
   - Added `@Transactional(readOnly = true) public IncidentServiceTracking getOrEvaluateTrackingReadOnly(Incident incident)`.
   - Computes SLA due date, elapsed time, and status in-memory for read operations without executing mutating `save()` calls.
   - Refactored `evaluateServiceTracking(Incident incident)` to delegate to `getOrEvaluateTrackingReadOnly` and save only in mutating contexts.
   - Guarded against null `incident.getCreatedAt()` with defensive fallback to `LocalDateTime.now()`.
2. **`nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/service/IncidentService.java`**
   - Updated `mapToEnrichedResponse()` to call `serviceRuleService.getOrEvaluateTrackingReadOnly(incident)`.
   - Ensures `getAllIncidents()` and `getIncidentById()` are 100% read-only safe without triggering PostgreSQL read-only transaction violations.
3. **`nirvaha-backend/src/test/java/com/nirvaha/nirvaha_backend/DashboardEmptyStateRegressionTest.java`**
   - Added 8 comprehensive regression tests verifying:
     - `OFFICER` and `ADMIN` can retrieve official incidents without HTTP 500.
     - `CITIZEN` is strictly forbidden (HTTP 403) from official dashboard endpoints.
     - Simulated incidents are separated from official incidents by default (`includeSimulated=false`).
     - Passing `includeSimulated=true` includes simulated records with explicit `classificationTag`.
     - Valid status enum strings (`REPORTED`, `VERIFIED`, `IN_PROGRESS`, `RESOLVED`) are exposed in API responses.
     - `getIncidentById` works safely in read-only transactions for un-tracked incidents.

### 4.2 Frontend Changes
1. **`nirvaha-frontend/src/services/index.js`**
   - Updated `getAllIncidents(includeSimulated = false)` and `getQueue(includeSimulated = false)` to accept the `includeSimulated` parameter and append `?includeSimulated=true` when requested.
2. **`nirvaha-frontend/src/pages/government/CommandCenter.jsx`**
   - Fixed status filtering for `activeCases`: checks for `REPORTED`, `VERIFIED`, and `IN_PROGRESS`.
   - Fixed `resolvedCases`: checks for `RESOLVED`.
   - Fixed `overdueSlaCount`: checks for `OVERDUE`.
   - Updated `statusFilter` options to match backend enum constants (`REPORTED`, `VERIFIED`, `IN_PROGRESS`, `RESOLVED`).
   - Distinct empty-state messaging:
     - If `incidents.length === 0`: Displays `"No Incidents Reported — All civic systems operational in Puducherry."`
     - If `incidents.length > 0 && filteredIncidents.length === 0`: Displays `"No matching incidents found for selected filters."` with a reset button.
   - Added explicit "Showing: Simulated + Official" toggle and disclaimer banner when demo scenarios are active.
   - Displays real API error details (e.g. 401/403) instead of masking backend status.
3. **`nirvaha-frontend/src/components/demo/JudgeDemoModal.jsx`**
   - Updated `onRefreshData(simFlag)` to notify parent components when simulated scenarios are loaded or reset.
4. **`nirvaha-frontend/src/pages/government/PriorityQueue.jsx`**
   - Added dedicated clean empty state when `incidents.length === 0` ("Priority Queue Clear — No unassigned or active incidents requiring immediate dispatch").

---

## 5. Before / After Verification Results

### 5.1 Backend Verification
- **Regression Suite:**
  ```
  mvn test -Dtest=DashboardEmptyStateRegressionTest
  Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 [SUCCESS]
  ```
- **Full Test Suite:**
  ```
  mvn test
  Tests run: 314, Failures: 0, Errors: 0, Skipped: 0 [SUCCESS]
  ```

### 5.2 Frontend Verification
- **Production Build:**
  ```
  npm run build
  vite v8.2.2 building client environment for production...
  ✓ 1944 modules transformed.
  ✓ built in 1.75s
  Exit code: 0
  ```

### 5.3 Behavior Comparison

| Scenario | Before Fix | After Fix |
| :--- | :--- | :--- |
| **New Incident Created (REPORTED)** | "Active Cases" metric showed **0**. Card was excluded from active filters. | "Active Cases" metric accurately increments. Card displays under "Active Cases" and `REPORTED` filter. |
| **Incident Without Tracking Record** | `GET /api/incidents` returned **HTTP 500** (`cannot execute INSERT in a read-only transaction`). | `GET /api/incidents` returns **HTTP 200 OK** with dynamically calculated SLA tracking. |
| **No Incidents in Database** | Showed misleading error: *"No open incidents match your search or filter criteria. Reset all filters"*. | Shows genuine empty state: *"No Incidents Reported — All civic systems operational in Puducherry."* |
| **Judge Demo Scenario Triggered** | Dashboard remained completely empty because simulated records were silently filtered out. | Dashboard displays simulated incidents with a distinct purple disclaimer banner and `[SIMULATED DEMO]` badge. Toggle allows viewing official-only or all. |

---

## 6. Remaining Limitations & Boundaries Preserved

1. **Simulated vs. Official Data Boundary:**
   Simulated records remain strictly isolated in `IncidentRepository.findOfficialIncidents()`. Simulated data is never presented as genuine citizen complaints; official reports remain untouched.
2. **Citizen Privacy & Role Separation:**
   Citizen reports cannot be viewed or accessed via citizen credentials on officer endpoints (`/api/incidents`, `/api/action-center/queue` return HTTP 403 Forbidden).
3. **SLA Escalation Scope:**
   In compliance with Phase 5A constraints, automatic SLA escalation notifications and cron jobs remain un-triggered until Phase 5B. Only the read-only SLA status tracking display was corrected.
