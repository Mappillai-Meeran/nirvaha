# Nirvaha Authentication Architecture (Phase 11A)

## 1. Overview
Phase 11A establishes the production-oriented **Account & Authentication Foundation** for Nirvaha. It equips the Spring Boot backend with stateless JWT authentication, Spring Security 3.4 filter chains, adaptive BCrypt password hashing, and account recovery lifecycle management.

> **Key Rule**: "Authentication establishes identity. Authorization & Role Security will be strictly enforced in Phase 11B."

---

## 2. Architecture & Components

```text
CLIENT (React + Vite)
   │
   ├─► POST /api/auth/login ────────► Spring Security DaoAuthenticationProvider
   │                                           │
   │                                           ├─► BCrypt Password Validation
   │                                           ├─► Legacy SHA-256 Check & Migration
   │                                           └─► Issue Signed JWT Token
   │
   └─► Authorization: Bearer <JWT> ──► JwtAuthenticationFilter
                                               │
                                               └─► Populate SecurityContextHolder
```

### Backend Components
* **`SecurityConfig.java`**: Configures stateless session management, BCrypt password encoder, and permits public auth routes while establishing JWT authentication filters.
* **`JwtService.java`**: Generates and parses signed JWT tokens containing `userId`, `email`, and `role` claims. Configured via `nirvaha.jwt.secret`.
* **`JwtAuthenticationFilter.java`**: Intercepts incoming `Authorization: Bearer <token>` headers, extracts claims, and initializes Spring Security authentication context.
* **`AuthService.java`**: Orchestrates login, user profile verification (`/api/auth/me`), BCrypt verification, legacy SHA-256 hash auto-migration, password changes, and reset token generation.

---

## 3. Password Security & Legacy SHA-256 Migration
* **New Registrations**: All new users created via `UserService` use `BCryptPasswordEncoder`.
* **Legacy Hash Auto-Migration**: If a user record contains a legacy SHA-256 hash, `AuthService` verifies the raw password against SHA-256. Upon successful authentication, it automatically re-encodes the password into BCrypt and updates the database transparently.

---

## 4. REST API Endpoints

| Endpoint | Method | Public / Auth | Description |
| :--- | :---: | :---: | :--- |
| `/api/auth/login` | `POST` | Public | Authenticates credentials and returns JWT + safe `UserResponse`. |
| `/api/auth/me` | `GET` | Authenticated | Returns currently authenticated user's safe profile. |
| `/api/auth/change-password` | `POST` | Authenticated | Updates account password after verifying current password. |
| `/api/auth/forgot-password` | `POST` | Public | Initiates email password recovery and issues short-lived reset token. |
| `/api/auth/reset-password` | `POST` | Public | Resets account password using a valid reset token. |

---

## 5. Environment & Configuration
JWT signing secrets are configurable in `application.properties`:

```properties
nirvaha.jwt.secret=${JWT_SECRET:nirvahaSecretKeyForLocalDevelopmentDoNotUseInProductionSuperSecretKey12345!}
nirvaha.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}
```

---

## 6. Phase 11B Readiness
Phase 11A establishes JWT identity. The system is ready for **Phase 11B — Authorization & Role Security**, which will enforce strict endpoint permissions (`CITIZEN`, `OFFICER`, `ADMIN`) across all Phase 1–10 endpoints.
