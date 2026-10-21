# Nirvaha — API Security Documentation

## Overview
This document details the security controls implemented in Nirvaha to protect exposed REST APIs, manage CORS policy, sanitize runtime error responses, enforce in-process rate limiting, and prevent parameter tampering / role escalation.

---

## 1. CORS Configuration

Nirvaha's CORS policy is integrated into Spring Security (`SecurityConfig.java`) using explicit configuration properties rather than wildcards.

- **Configuration Property**: `nirvaha.allowed-origins` (Environment variable: `NIRVAHA_ALLOWED_ORIGINS`)
- **Default Development Origins**: `http://localhost:5173`, `http://localhost:3000`
- **Allowed HTTP Methods**: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- **Allowed Headers**: `Authorization`, `Content-Type`, `Accept`
- **Allow Credentials**: `true` (Wildcard `*` origins are strictly prohibited when credentials are enabled).

---

## 2. Security Headers

Spring Security enforces the following HTTP security response headers:
- `X-Content-Type-Options: nosniff`: Prevents MIME-sniffing of responses.
- `X-Frame-Options: DENY`: Protects against clickjacking attacks.
- `Referrer-Policy: strict-origin-when-cross-origin`: Restricts referrer disclosure across domains.

---

## 3. Standardized Error Handling

All controller exceptions and HTTP error responses follow a uniform JSON structure defined in `GlobalExceptionHandler.java`:

```json
{
  "timestamp": "2026-09-21T16:15:00.123456",
  "status": 400,
  "error": "Bad Request",
  "message": "Sanitized error message",
  "path": "/api/incidents"
}
```

### HTTP Status Code Rules
- **400 Bad Request**: Validation failures (`MethodArgumentNotValidException`), malformed payloads, or illegal request parameters.
- **401 Unauthorized**: Unauthenticated request to protected endpoints handled via Spring Security `AuthenticationEntryPoint`.
- **403 Forbidden**: Insufficient privileges / role mismatch handled via `AccessDeniedHandler` or `AccessDeniedException`.
- **404 Not Found**: Resource lookup failure (`ResourceNotFoundException`).
- **409 Conflict**: Duplicate resources (`DuplicateResourceException`).
- **413 Payload Too Large**: File size exceeding maximum limit (`MaxUploadSizeExceededException`).
- **429 Too Many Requests**: Rate limit exceeded (`RateLimitingFilter`).
- **500 Internal Server Error**: Catches all unhandled exceptions. Sanitized output strictly returns `"An unexpected internal error occurred."` without exposing stack traces, SQL syntax, class names, or database internals.

---

## 4. Rate Limiting & Abuse Protection

An in-process `ConcurrentHashMap`-based token bucket rate limiter is implemented in `RateLimitingFilter.java`.

- **Configuration Properties**:
  - `nirvaha.rate-limit` (Default: 60 requests)
  - `nirvaha.rate-limit-window` (Default: 60 seconds)
- **Protected Endpoints**:
  - `POST /api/auth/login`
  - `POST /api/auth/forgot-password`
  - `POST /api/auth/reset-password`
  - `POST /api/users` (Registration)
  - `POST /api/incidents`
  - `POST /api/evidence/**`
  - `POST /api/incidents/{id}/resolutions`
- **Identity Resolution**:
  - Authenticated requests: Bound to the authenticated principal username (`auth.getName()`).
  - Unauthenticated requests: Bound to client IP (`X-Forwarded-For` or `request.getRemoteAddr()`).
- **Memory Bounded Cleanup**: Evicts stale token buckets that haven't been accessed for more than twice the window duration when key count exceeds 1,000 entries.

---

## 5. Actor Identity & Parameter Tampering Protection

- Server-side derivation of reporter/officer identity from the authenticated `SecurityContextHolder` principal.
- Client attempts to override `reporterId` or `officerId` in DTO payloads are checked against the authenticated identity.
- Public user registration (`POST /api/users`) strictly forces `ROLE_CITIZEN` regardless of any `role` field present in the JSON request body, unless invoked by an authenticated `ROLE_ADMIN`.
