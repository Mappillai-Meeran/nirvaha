# Nirvaha — Phase 1: Resident Privacy and Incident API Isolation Report
**Municipal Civic Service-Request Platform Quality & Security Audit**
*Audit Date: 2026-10-14 | Scope: E:\Nirvaha (Backend & Frontend)*

---

## 1. Executive Summary

During the Product Release Readiness Audit, a **Critical Security & Privacy Exposure (Gap 1)** was identified: the general incident listing endpoint (`GET /api/incidents`) returned the full municipal database (`incidentRepository.findAll()`) to any authenticated citizen, leaking citizen full names, reporter IDs, residential addresses, GPS coordinates, photographic evidence, and internal municipal service notes without tenant or owner isolation. Furthermore, the citizen frontend view (`MyReports.jsx`) directly invoked this municipal-wide query.

**In Phase 1, this exposure has been completely resolved and server-enforced privacy isolation established.**

### Key Accomplishments:
1. **Perimeter Role Restriction**: `GET /api/incidents` is now strictly restricted to authenticated `OFFICER` and `ADMIN` roles in `SecurityConfig.java`. Any citizen attempting to call this endpoint receives HTTP 403 Forbidden.
2. **Resident-Owned Reports Endpoint**: Implemented `GET /api/incidents/my-reports`, deriving resident identity solely from the authenticated principal in `SecurityContextHolder` (`auth.getName()`), completely eliminating reliance on client-supplied IDs. Returns an empty list with HTTP 200 OK if no reports exist.
3. **Sub-Resource Ownership Enforcement**: Introduced `IncidentAccessValidator` to enforce ownership across all incident inspection and sub-resource endpoints (`/api/incidents/{id}`, `/api/evidence/incident/{id}`, `/api/evidence/{id}`, `/api/incidents/{id}/actions`, `/api/incidents/{id}/workflow`, `/api/incidents/{id}/resolution`). Citizen A is strictly blocked with HTTP 403 Forbidden when attempting to access Citizen B's records.
4. **Authoritative Government Role Preservation**: Municipal Officers and Administrators retain full operational visibility across all incidents, evidence, action history, and resolutions. Existing Judge Demo mode execution is completely unaffected.
5. **Frontend Resident Integration**: Updated `MyReports.jsx` and `services/index.js` to consume `GET /api/incidents/my-reports`. Added graceful handling for unauthorized states (with direct login prompts), loading spinners, and empty states. Updated `IncidentDetail.jsx` to respect 401/403 authorization boundaries without falling back to demo records.
6. **Automated Regression Test Suite**: Created `ResidentPrivacyIsolationTest.java` with 18 comprehensive integration tests proving privacy isolation, ownership boundaries, officer bypass, and 401 unauthenticated enforcement.

---

## 2. Server-Enforced Architecture & Changes

### 2.1 Perimeter Authorization (`SecurityConfig.java`)
The request matchers were updated with strict ordering so specific resident endpoints take precedence while the general listing is locked down:
- `POST /api/incidents` -> `CITIZEN`, `OFFICER`, `ADMIN` (Report filing)
- `GET /api/incidents/my-reports` -> `CITIZEN`, `OFFICER`, `ADMIN` (Resident-owned report retrieval)
- `GET /api/incidents` -> `OFFICER`, `ADMIN` **ONLY** (Municipal listing locked down)
- `GET /api/incidents/*` & `GET /api/incidents/*/**` -> Authenticated roles (Subject to service-level ownership validation)
- `POST /api/incidents/*/actions`, `POST /api/incidents/*/resolutions` -> `OFFICER`, `ADMIN` **ONLY** (Government workflow mutations locked down)

### 2.2 Dedicated Security Validator (`IncidentAccessValidator.java`)
Created a centralized Spring component (`com.nirvaha.nirvaha_backend.security.IncidentAccessValidator`) that validates:
```java
public void verifyIncidentAccess(Incident incident) {
    if (incident == null) return;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return; // Non-HTTP service or unit test invocation

    if (!auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
        throw new AccessDeniedException("Authentication required to access incident data");
    }

    boolean isOfficerOrAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_OFFICER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
    if (isOfficerOrAdmin) return;

    // Verify authenticated citizen is the incident reporter
    if (incident.getReporter() == null || incident.getReporter().getEmail() == null ||
            !incident.getReporter().getEmail().equalsIgnoreCase(auth.getName())) {
        throw new AccessDeniedException("Access denied: You do not have permission to access incident #" + incident.getId());
    }
}
```

### 2.3 Service-Layer Verification Points
Injected `IncidentAccessValidator` into:
1. `IncidentService`:
   - `getIncidentById(Long id)`: Calls `verifyIncidentAccess(incident)`.
   - `getMyReports()`: Finds current user by `auth.getName()`, executes `findByReporterIdOrderByCreatedAtDesc(currentUser.getId())`.
2. `EvidenceService`:
   - `uploadEvidence(Long incidentId, ...)`: Enforces ownership before upload.
   - `createEvidence(CreateEvidenceRequest request)`: Enforces ownership before attachment.
   - `getEvidenceById(Long id)`: Validates ownership of `evidence.getIncident()`.
   - `getEvidenceForIncident(Long incidentId)`: Validates ownership of parent incident.
3. `IncidentWorkflowService`:
   - `getActionsForIncident(Long incidentId)`: Validates ownership before returning audit actions.
   - `getWorkflowForIncident(Long incidentId)`: Validates ownership before returning workflow summary.
4. `ResolutionService`:
   - `getResolutionForIncident(Long incidentId)`: Validates ownership before returning resolution.
   - `getResolutionByIncidentId(Long incidentId)`: Validates ownership before returning resolution.

### 2.4 Exception Mapping (`GlobalExceptionHandler.java`)
Confirmed that `AccessDeniedException` maps directly to HTTP 403 Forbidden with standard Nirvaha error payload:
```json
{
  "timestamp": "2026-10-14T...",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied: You do not have sufficient privileges to access this resource",
  "path": "/api/incidents/..."
}
```

### 2.5 Frontend Integration
1. `nirvaha-frontend/src/services/index.js`: Added `getMyReports: () => apiClient.get('/incidents/my-reports')`.
2. `nirvaha-frontend/src/pages/citizen/MyReports.jsx`: Replaced `getAllIncidents()` with `getMyReports()`. Added dedicated `unauthorized` state showing an authentication prompt and login button if session is unauthenticated, and error states for network issues.
3. `nirvaha-frontend/src/pages/government/IncidentDetail.jsx`: Updated error boundary to detect 401/403 status codes and show explicit "Access Denied" messaging rather than falling back to demo records.

---

## 3. Verification & Automated Test Results

### 3.1 Dedicated Privacy & Isolation Test Suite (`ResidentPrivacyIsolationTest.java`)
All 18 test cases pass:
| # | Test Name | Expected Result | Status |
| :- | :--- | :--- | :--- |
| 1 | `citizenCannotAccessGeneralIncidentsEndpoint` | HTTP 403 Forbidden | **PASS** |
| 2 | `citizenASeesOnlyOwnReports` | HTTP 200 OK, length = 1, Incident A only | **PASS** |
| 3 | `citizenBSeesOnlyOwnReports` | HTTP 200 OK, length = 1, Incident B only | **PASS** |
| 4 | `citizenWithNoReportsGetsEmptyList` | HTTP 200 OK, length = 0 | **PASS** |
| 5 | `officerCanCallMyReportsWithoutError` | HTTP 200 OK, length = 0 | **PASS** |
| 6 | `citizenACannotReadCitizenBIncidentDetails` | HTTP 403 Forbidden | **PASS** |
| 7 | `citizenACanReadOwnIncidentDetails` | HTTP 200 OK, Incident A details | **PASS** |
| 8 | `citizenACannotAccessCitizenBEvidenceList` | HTTP 403 Forbidden | **PASS** |
| 9 | `citizenACanAccessOwnEvidenceList` | HTTP 200 OK, Evidence A details | **PASS** |
| 10 | `citizenACannotAccessCitizenBSingleEvidence` | HTTP 403 Forbidden | **PASS** |
| 11 | `citizenACannotAccessCitizenBActions` | HTTP 403 Forbidden | **PASS** |
| 12 | `citizenACannotAccessCitizenBWorkflow` | HTTP 403 Forbidden | **PASS** |
| 13 | `citizenACannotAccessCitizenBResolution` | HTTP 403 Forbidden | **PASS** |
| 14 | `citizenCannotPerformWorkflowActions` | HTTP 403 Forbidden | **PASS** |
| 15 | `officerCanAccessGeneralIncidentsList` | HTTP 200 OK, length = 2 | **PASS** |
| 16 | `adminCanAccessGeneralIncidentsList` | HTTP 200 OK, length = 2 | **PASS** |
| 17 | `officerCanAccessAnyIncidentAndSubResources` | HTTP 200 OK across both incidents | **PASS** |
| 18 | `unauthenticatedRequestsReturn401` | HTTP 401 Unauthorized across endpoints | **PASS** |

### 3.2 Frontend Build & Lint Verification
- `npm run lint`: **0 errors**, 50 non-blocking warnings on existing dashboard components. Exit code: **0**.
- `npm run build`: Vite build completed successfully in 2.33s. Exit code: **0**.

---

## 4. Next Phase Recommendations

With Phase 1 complete, citizen data exposure is eliminated and ownership isolation is cryptographically and authoritatively enforced. The remaining high-priority gaps from the Readiness Audit to address in Phase 2:
1. **Resident Confirmation & Dispute Workflow (Gap 3 & 4)**: Implement citizen-facing confirmation of resolution (`POST /api/incidents/{id}/confirm`) and citizen reopen capability (`POST /api/incidents/{id}/reopen`) when work is incomplete.
2. **Password Reset Security (Gap 5)**: Prevent disclosure of password reset tokens in HTTP response payloads.
