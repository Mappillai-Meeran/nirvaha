# Nirvaha — Production Configuration & Hardening Guide (Phase 11D)

This document describes the environment configuration, profile management, deployment security controls, operational health checks, and production hardening implemented for the Nirvaha civic intelligence platform.

---

## 1. Profile Separation

Nirvaha supports clean profile separation using standard Spring Boot configuration properties:

- **`dev` Profile** (Default profile for local development):
  - Uses `application-dev.properties`.
  - Enables Hibernate dynamic schema creation (`spring.jpa.hibernate.ddl-auto=update`).
  - Enables SQL formatting and debug logging (`logging.level.com.nirvaha=DEBUG`).
  - Provides safe local development defaults.

- **`prod` Profile** (For staging and production deployments):
  - Activated via `SPRING_PROFILES_ACTIVE=prod`.
  - Uses `application-prod.properties`.
  - Enforces mandatory environment variables without silent development fallbacks.
  - Locks JPA schema DDL mutation to `validate` (`spring.jpa.hibernate.ddl-auto=validate`).
  - Disables SQL query printing (`spring.jpa.show-sql=false`).
  - Configures HikariCP connection pooling bounds and strict JWT secret validation.

---

## 2. Supported Environment Variables

| Variable Name | Description | Dev Default | Production Requirement |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | Must be set to `prod` |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/nirvaha_db` | Mandatory (No fallback) |
| `DATABASE_USERNAME` | PostgreSQL username | `postgres` | Mandatory (No fallback) |
| `DATABASE_PASSWORD` | PostgreSQL password | `meeran20` | Mandatory (No fallback) |
| `JWT_SECRET` | HMAC-SHA256 signing key | Development fallback string | Mandatory (Min 32 bytes/256 bits) |
| `JWT_EXPIRATION` | Token expiration in ms | `86400000` (24h) | Optional override |
| `NIRVAHA_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:5173,http://localhost:3000` | Mandatory deployed frontend origin |
| `NIRVAHA_MAX_EVIDENCE_SIZE` | Evidence upload size limit | `10485760` (10MB) | Optional (Default: 10MB) |
| `NIRVAHA_RATE_LIMIT` | Token bucket max requests | `60` | Optional (Default: 60) |
| `NIRVAHA_RATE_LIMIT_WINDOW`| Token bucket window (sec) | `60` | Optional (Default: 60) |
| `NIRVAHA_EVIDENCE_STORAGE_PATH` | Evidence storage dir | `uploads/evidence` | Persistent directory path |
| `PORT` | Backend HTTP server port | `8080` | Deployment server port |
| `VITE_API_BASE_URL` | Frontend API Base URL | `http://localhost:8080/api` | Deployed backend API URL |

---

## 3. JWT Security & Fail-Fast Hardening

In production profile (`SPRING_PROFILES_ACTIVE=prod`), `JwtService` executes startup post-construct validation:
- Checks if `JWT_SECRET` is missing, empty, or using the default development secret key.
- Validates that the secret key length is at least 32 bytes (256 bits) required for HMAC-SHA256 signatures.
- **Fail-Fast**: If validation fails, application startup terminates immediately with `IllegalStateException`, preventing insecure deployment.

---

## 4. Database Connection Pool & JPA Safety

Production configuration tunes the HikariCP connection pool for optimal performance and bounded resource usage:
- `maximum-pool-size`: 20 connections
- `minimum-idle`: 5 connections
- `connection-timeout`: 30,000 ms
- `idle-timeout`: 600,000 ms (10 min)
- `max-lifetime`: 1,800,000 ms (30 min)

JPA schema safety prevents runtime schema corruption:
- `spring.jpa.hibernate.ddl-auto=validate` (ensures database schema matches entity mappings without making unauthorized schema mutations).

---

## 5. Operations & Health Visibility

- **Health Endpoint**: `/actuator/health` is exposed publicly for load balancers and deployment orchestrators.
- **Sanitized Output**: `management.endpoint.health.show-details=never` ensures database credentials, internal system properties, and component details are not exposed to unauthenticated callers.
- **Graceful Shutdown**: `server.shutdown=graceful` is configured to allow in-flight HTTP requests to finish cleanly during deployment rolling restarts.

---

## 6. Frontend Production API Configuration

- Frontend builds use Vite environment configuration (`.env.development`, `.env.production`).
- Base URL is configured dynamically via `import.meta.env.VITE_API_BASE_URL`.
- Build command: `npm run build` generates production assets in `dist/`.

---

## 7. Pre-Deployment Verification Checklist

- [x] All 151 backend automated unit and integration tests pass with 0 failures, 0 errors.
- [x] Frontend `npm run build` succeeds without bundle errors.
- [x] Backend verified with `SPRING_PROFILES_ACTIVE=prod` and environment secrets.
- [x] `/actuator/health` responds `{"status":"UP"}` without leaking details.
- [x] User registration, JWT authentication, `/api/auth/me`, incident creation, and evidence upload verified under production profile.
- [x] Security headers (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`) verified active.
- [x] CORS preflight rejects unauthorized origins and permits configured `NIRVAHA_ALLOWED_ORIGINS`.
- [x] Unauthenticated endpoints correctly return 401 Unauthorized with sanitized JSON error contract.
