# NIRVAHA — AUTHORIZATION & ROLE SECURITY (PHASE 11B)

## Overview & Architecture

Phase 11B implements **Authorization & Role Security** across the Nirvaha Civic Intelligence Platform.
While Phase 11A established identity authentication ("Who are you?"), Phase 11B enforces access rights ("What are you allowed to do?").

Nirvaha operates two separate security layers:

```
JWT Token → SecurityContext → UserRole (RBAC) → Principal Ownership → Business Logic
```

1. **Role-Based Access Control (RBAC)**: Checks if the user's role (`ROLE_CITIZEN`, `ROLE_OFFICER`, `ROLE_ADMIN`) permits the requested endpoint operation.
2. **Server-Side Ownership & Principal Mapping**: Ensures operations act strictly on behalf of the authenticated user (derived from `SecurityContextHolder.getContext().getAuthentication()`), preventing client-side ID spoofing or role escalation via request bodies or localStorage manipulation.

---

## 1. Security Matrix & Endpoint Access Rules

| Endpoint / Operation | HTTP Method | Required Role | Scope / Filtering |
| :--- | :--- | :--- | :--- |
| `/api/auth/**` | POST / GET | `PERMIT_ALL` | Public authentication (Login, Register, Password Reset) |
| `/api/auth/me` | GET | `CITIZEN`, `OFFICER`, `ADMIN` | Returns current authenticated principal profile |
| `/api/incidents` | GET | `CITIZEN`, `OFFICER`, `ADMIN` | Authenticated read. Officers/Admins see all; Citizens see filtered public data |
| `/api/incidents/{id}` | GET | `CITIZEN`, `OFFICER`, `ADMIN` | Authenticated read |
| `/api/incidents` | POST | `CITIZEN`, `OFFICER`, `ADMIN` | Reporter identity mapped strictly from JWT Principal |
| `/api/incidents/{id}/workflow` | POST / GET | `OFFICER`, `ADMIN` | Government workflow execution (Verify, Start Work, Reopen) |
| `/api/incidents/{id}/resolutions` | POST / GET | `OFFICER`, `ADMIN` | Government resolution submission & similarity assessment |
| `/api/integrity/**` | GET | `OFFICER`, `ADMIN` | Government integrity radar & audit logs |
| `/api/risk/**` | GET / POST | `OFFICER`, `ADMIN` | Government risk intelligence engine & ward risk evaluations |

---

## 2. Server-Side Ownership & Identity Enforcement

To prevent identity spoofing, the backend derives actors directly from the authenticated JWT principal rather than trusting client-supplied IDs:

- **Incident Creation (`POST /api/incidents`)**: The `reporter` entity is resolved using `auth.getName()` (email). Frontend-supplied `reporterId` fields are overridden by the server.
- **Workflow Actions (`POST /api/incidents/{id}/workflow`)**: Acting officers are bound to the authenticated user principal. Non-government users attempting workflow actions receive `403 Forbidden` / business validation errors.
- **Resolution Evidence Submission (`POST /api/incidents/{id}/resolutions`)**: Officer identity is enforced server-side from `SecurityContextHolder`.

---

## 3. Standardized Security Exception Responses

- **Unauthenticated Access (`401 Unauthorized`)**:
  ```json
  {
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource"
  }
  ```

- **Insufficient Role Rights (`403 Forbidden`)**:
  ```json
  {
    "status": 403,
    "error": "Forbidden",
    "message": "Access Denied: You do not have sufficient privileges to access this resource"
  }
  ```

---

## 4. Verification & Audit Metrics

- **Backend Test Suite Baseline**: 128 tests passing (`0` failures, `0` errors, `0` skipped).
- **Dedicated Authorization Test Suite**: `AuthorizationSecurityTest` verifies:
  - `401 Unauthorized` for unauthenticated protected requests.
  - `403 Forbidden` for citizens attempting to access restricted Government APIs (`/api/risk/wards`, `/api/integrity/anomalies`, `/api/incidents/{id}/workflow`, `/api/incidents/{id}/resolutions`).
  - `200 OK` for authorized Officers and Admins accessing operational endpoints.
  - Ownership binding prevents client-supplied reporter/officer ID spoofing.
- **Frontend Build**: Production build (`vite build`) compiled cleanly without errors.
