# Nirvaha Phase 2 — Secure Password Recovery Remediation Report

**Date**: October 14, 2026  
**Auditor / Engineer**: Antigravity Autonomous Security Agent  
**Target Repository**: `E:\Nirvaha` (`nirvaha-backend` & `nirvaha-frontend`)  
**Status**: **COMPLETED & VERIFIED** (240 Backend Tests Passing, 0 Frontend Errors)

---

## 1. Executive Summary & Objective

In the initial release readiness audit, **Gap 5 (High Severity Security Vulnerability)** documented an insecure password recovery implementation: `POST /api/auth/forgot-password` generated an in-memory UUID token and returned it directly in the HTTP JSON response payload (`Map.of("message", "...", "resetToken", resetToken)`), allowing anyone knowing a user's email address to immediately hijack their account.

### Remediated Security Outcomes
1. **Zero Usable Token Exposure**: Reset tokens are never returned in API response bodies, URLs, error logs, or browser-visible properties.
2. **Cryptographic Unpredictability & Server-Side Hashing**: Tokens are generated via `java.security.SecureRandom` (256-bit entropy, 64 hex characters). Only SHA-256 hashes are persisted in the PostgreSQL database (`password_reset_tokens`).
3. **Verified Delivery Capability & No Fake Delivery**: Inspection of the codebase verified that no outbound email (SMTP) or SMS provider is currently provisioned or configured. Public self-service recovery token issuance is safely disabled in the UI; users receive an advisory explaining the limitation and directing them to municipal administrative channels.
4. **Anti-Enumeration Protections**: Consistent HTTP 200 OK responses with identical message payloads are returned for both existing and non-existing email addresses. Constant-time dummy hashing is executed when an email does not exist to eliminate timing side-channels.
5. **Token Lifecycle Enforcement**: Single-use enforcement (`used = true`), strict 15-minute expiration windows, and rejection of invalid, expired, reused, or tampered tokens with HTTP 400 Bad Request.
6. **Rate Limiting & Abuse Prevention**: Dedicated rate limiter bucket enforcing a threshold of 5 requests per 60 seconds per IP on password recovery endpoints (`/api/auth/forgot-password` and `/api/auth/reset-password`).
7. **Preservation of Phase 1 Controls**: Zero regressions to Phase 1 resident incident privacy isolation and role-based access control.

---

## 2. Delivery Channel Assessment

An inspection of `nirvaha-backend/pom.xml`, `application.properties`, and the service layer confirmed:
- No `spring-boot-starter-mail` dependency.
- No JavaMailSender or SMTP configurations (`spring.mail.*`).
- No SMS gateway client (Twilio, AWS SNS, etc.).

**Decision in compliance with Requirements 3 & 4**:
We did not introduce hardcoded credentials or simulate fake email delivery. Instead:
- Server logs record that recovery was initiated for the account (without logging the token or personal identifiers).
- The public API returns an informative, uniform message stating that email delivery is currently not configured on this municipal server and advising users to contact municipal administration.
- The UI explains this infrastructure state without leaking tokens.
- An administrative token entry mode is provided in the frontend so authorized recovery tokens issued through verified offline administrative workflows can be safely redeemed.

---

## 3. Architecture & Security Implementation Details

### A. Database Migration (`V2__password_reset_tokens.sql`)
A dedicated PostgreSQL table `password_reset_tokens` was introduced with a foreign key constraint to `users(id)`:
```sql
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_hash ON password_reset_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user ON password_reset_tokens(user_id);
```

### B. Entity & Repository
- `PasswordResetToken.java`: JPA Entity tracking `tokenHash`, `user`, `expiryDate`, `used`, `usedAt`, and `createdAt`, with a helper `isExpired()` method comparing against `LocalDateTime.now()`.
- `PasswordResetTokenRepository.java`: Queries `findByTokenHash(String tokenHash)` and `deleteByUser(User user)`.

### C. Service Layer (`AuthService.java`)
- **`initiateForgotPassword(ForgotPasswordRequest request)`**:
  - Validates email input.
  - Queries `userRepository.findByEmail(request.getEmail())`.
  - When user is present: generates 32 random bytes from `SecureRandom`, encodes to 64 lowercase hex characters, computes SHA-256 hash, invalidates prior unused tokens for this user, and stores the new hash with `expiryDate = now + 15 minutes`.
  - When user is absent: executes a constant-time dummy SHA-256 computation to ensure response timing does not reveal account existence.
  - Returns the raw token internally (for internal/admin/test usage); **never exposed by the controller**.
- **`resetPassword(ResetPasswordRequest request)`**:
  - Computes SHA-256 hash of `request.getToken()`.
  - Looks up token record in `PasswordResetTokenRepository`.
  - Rejects if token not found, `used == true`, or `expiryDate.isBefore(now)` with `IllegalArgumentException("Invalid or expired password reset token")`.
  - Updates the associated `User`'s password hash using `passwordEncoder.encode(request.getNewPassword())`.
  - Sets `token.setUsed(true)` and `token.setUsedAt(LocalDateTime.now())`.

### D. Controller Boundary (`AuthController.java`)
- `POST /api/auth/forgot-password`:
  - Strictly ignores internal raw token return value.
  - Unconditionally returns HTTP 200 OK:
    ```json
    {
      "message": "If an account with that email exists, password reset instructions have been initiated. Currently, automated external email delivery is not configured on this municipal server. Please contact your municipal administrator for password assistance."
    }
    ```
- `POST /api/auth/reset-password`:
  - Accepts `{ "token": "...", "newPassword": "..." }`.
  - Returns HTTP 200 OK on success or HTTP 400 Bad Request on invalid/expired token.

### E. Abuse Control & Rate Limiting (`RateLimitingFilter.java`)
- Added specialized detection for password recovery endpoints:
  ```java
  private boolean isPasswordRecoveryEndpoint(String path, String method) {
      if ("POST".equalsIgnoreCase(method)) {
          return path.equals("/api/auth/forgot-password") ||
                 path.equals("/api/auth/reset-password");
      }
      return false;
  }
  ```
- Configured dedicated token bucket allowing **5 requests per 60 seconds per IP** (`nirvaha.recovery.rate-limit`).
- Returns `HTTP 429 Too Many Requests` with a descriptive payload and `Retry-After: 60` header when threshold is exceeded.

### F. Frontend Implementation (`ForgotPassword.jsx`)
- Removed the previous insecure state assignment: `setResetToken(res.resetToken)`.
- Displays an official advisory banner explaining the municipal infrastructure status and next steps.
- Provides an optional "Have a recovery token?" toggle allowing residents or municipal staff with an administratively issued token to securely submit `{ token, newPassword, confirmPassword }`.
- Validates password length (>= 6 chars) and confirmation match before API submission.
- Fully styled with Nirvaha's municipal color palette (`#163A36`, `#2F6F68`, `#C87941`).

---

## 4. Session Invalidation & Stateless JWT Analysis

### Current Architectural Reality
- Nirvaha issues stateless HMAC-SHA256 JWTs signed with a static secret (`nirvaha.jwt.secret`) valid for 24 hours (`86,400,000 ms`).
- There is currently **no centralized session store or token revocation blocklist** (e.g., Redis blacklisting, DB revocation table, or user token versioning).
- When a user's password is reset:
  - The **password reset token itself is immediately invalidated** (`used = true`) and cannot be reused.
  - However, **any pre-existing JWTs issued prior to the reset will remain valid for API requests until their 24-hour expiration**.

### Future Recommendation (Post-Release)
Implement a `jwt_token_version` column on the `users` table or a Redis-backed token revocation blacklist so that password resets increment the token version, immediately rejecting older JWTs in `JwtAuthenticationFilter`.

---

## 5. Automated Verification Results

### A. Dedicated Security Test Suite (`PasswordRecoverySecurityTest`)
Location: `nirvaha-backend/src/test/java/com/nirvaha/nirvaha_backend/PasswordRecoverySecurityTest.java`

| Test Case | Method Name | Description | Result |
| :--- | :--- | :--- | :--- |
| **No Token in Response** | `forgotPasswordNeverReturnsTokenInResponse` | Confirms `$.resetToken` & `$.token` do not exist in payload; only uniform message returned | **PASSED** |
| **Anti-Enumeration** | `forgotPasswordUnknownAccountReturnsSameGenericResponse` | Non-existent email returns identical HTTP 200 OK & message without error | **PASSED** |
| **Valid Password Reset** | `validResetPasswordSucceeds` | Verifies SHA-256 hashed token lookup resets password successfully | **PASSED** |
| **Password Update Enforcement** | `passwordUpdateEnforcesNewPassword` | Verifies old BCrypt password fails login; new password authenticates | **PASSED** |
| **Token Reuse Block** | `tokenReuseIsBlocked` | Second attempt using same token returns HTTP 400 Bad Request | **PASSED** |
| **Expired Token Block** | `expiredTokenIsRejected` | Token with `expiryDate` in past returns HTTP 400 Bad Request | **PASSED** |
| **Tampered / Invalid Token** | `invalidOrTamperedTokenIsRejected` | Random / bogus token string returns HTTP 400 Bad Request | **PASSED** |
| **Abuse Rate Limiting** | `rateLimitingPreventsAbuseOnRecoveryEndpoints` | Exceeding 5 recovery requests in 60s window triggers HTTP 429 Too Many Requests | **PASSED** |

**Execution Log Summary**:
```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.71 s -- in com.nirvaha.nirvaha_backend.PasswordRecoverySecurityTest
[INFO] BUILD SUCCESS
```

### B. Full Backend Test Suite
Executed: `mvn test` across all 24 test classes.
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 240, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:16 min
```
Zero test regressions. All Phase 1 privacy tests (`ResidentPrivacyIsolationTest`, 18 tests) and all existing functional tests passed.

### C. Frontend Lint & Build
1. **Linting (`npm run lint` via `oxlint`)**:
   - Exit code: `0`
   - Warnings in codebase: 50 (pre-existing unused imports in unrelated files)
   - Warnings in `ForgotPassword.jsx`: **0**
   - Errors: **0**
2. **Build (`npm run build` via `vite build`)**:
   - Exit code: `0`
   - Output: `dist/` generated cleanly in 2.30s.

---

## 6. Changed Files Summary

| File | Type | Changes |
| :--- | :--- | :--- |
| `nirvaha-backend/src/main/resources/db/migration/V2__password_reset_tokens.sql` | SQL Migration | Schema for `password_reset_tokens` table with SHA-256 hash indexing and cascade foreign key |
| `nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/entity/PasswordResetToken.java` | Java Entity | JPA mapping with `tokenHash`, `expiryDate`, `used`, and `isExpired()` |
| `nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/repository/PasswordResetTokenRepository.java` | Java Repository | Spring Data JPA repository for hash lookup and user cleanup |
| `nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/service/AuthService.java` | Java Service | Cryptographic token generation, SHA-256 hashing, single-use invalidation, constant-time dummy hashing |
| `nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/controller/AuthController.java` | Java Controller | Removed token from response body; returns uniform generic advisory message |
| `nirvaha-backend/src/main/java/com/nirvaha/nirvaha_backend/security/RateLimitingFilter.java` | Java Filter | Added 5 req/min rate limiting on recovery endpoints with 429 response |
| `nirvaha-backend/src/test/java/com/nirvaha/nirvaha_backend/PasswordRecoverySecurityTest.java` | Java Test | 8 comprehensive automated security tests for password recovery |
| `nirvaha-backend/src/test/java/com/nirvaha/nirvaha_backend/AuthControllerTest.java` | Java Test | Updated tearDown and test setup to ensure clean token repository state |
| `nirvaha-frontend/src/pages/auth/ForgotPassword.jsx` | React Component | Removed token-in-response expectation, added municipal delivery advisory and manual token reset mode |

---

## 7. Recommended Next Phase

### Single Recommended Next Phase:
**Phase 3 — Resident Resolution Confirmation & Dispute Workflow (Gap 3 & Gap 4)**

**Rationale**:
With Gap 1, Gap 2 (Phase 1), and Gap 5 (Phase 2) fully resolved and audited, the municipal service request platform is securely isolated and authenticated. However, the core civic lifecycle remains incomplete: once an officer flags an incident as `RESOLVED`, the resident currently has no mechanism to confirm satisfaction or dispute the resolution if the physical work was defective or incomplete. Resolving Gaps 3 and 4 will close the feedback loop and complete the municipal accountability lifecycle.
