# Nirvaha — Evidence Security Documentation

## Overview
This document details the security model, file validation procedures, storage architecture, filename generation, and ownership controls implemented for evidence uploaded to Nirvaha during citizen incident reporting and officer resolution verification.

---

## 1. Secure File Validation (`FileSecurityValidator.java`)

All evidence files uploaded via multipart requests undergo strict four-tier validation before being saved to storage or referenced by database records:

### A. Supported Formats & MIME Types
- **JPEG**: MIME `image/jpeg`, extensions `.jpg`, `.jpeg`
- **PNG**: MIME `image/png`, extension `.png`
- **WEBP**: MIME `image/webp`, extension `.webp`
- Any file with an unlisted MIME type or extension is immediately rejected with HTTP 400 Bad Request.

### B. Magic Bytes Verification
The validator checks raw byte headers to ensure file content matches the declared MIME type:
- **JPEG Signature**: `FF D8 FF`
- **PNG Signature**: `89 50 4E 47`
- **WEBP Signature**: `RIFF....WEBP` (`52 49 46 46` header prefix)
Files with mismatched magic bytes (such as executable scripts renamed to `.jpg`) fail signature verification.

### C. Image Decoding Verification
The file payload is decoded using `ImageIO.read(InputStream)`. Files that fail structural decoding (corrupted image headers, partial downloads, malformed pixel blocks) are rejected.

### D. File Size Enforcing
- **Maximum File Size Property**: `nirvaha.max-evidence-size` (Environment variable: `NIRVAHA_MAX_EVIDENCE_SIZE`)
- **Default Limit**: 10 MB (10,485,760 bytes).
- Oversized uploads trigger an HTTP 413 Payload Too Large error before expensive image processing is executed.

---

## 2. Server-Controlled Filename & Path Security

To protect against path traversal and arbitrary file execution:
- Client-provided filenames (`file.getOriginalFilename()`) are NEVER used as filesystem storage keys.
- **Filename Generation**: A random UUID plus validated extension is generated server-side:
  `UUID.randomUUID().toString() + "." + extension`
  Example: `550e8400-e29b-41d4-a716-446655440000.jpg`
- Path traversal sequences (`../`, `..\`, encoded slashes, absolute paths) are completely rendered ineffective.

---

## 3. Storage Architecture

- **Storage Path Namespace**: `/uploads/evidence/{secureFilename}`
- **Storage Credentials**: No filesystem paths, credentials, or internal server directory locations are exposed to API consumers.
- **Object Storage Compatibility**: The evidence storage service abstracts file saving, making it fully compatible with future object storage providers (e.g., S3, Google Cloud Storage, Cloudinary).

---

## 4. Ownership & Access Control

- Evidence creation is bound to the authenticated principal in `SecurityContextHolder`.
- Citizens can only query/access evidence bound to their own incidents or public incident listings.
- Government Officers and Admins can access operational evidence across assigned incidents for verification and resolution audit trails.

---

## 5. Phase 7 Resolution Evidence Hardening

Officer-submitted resolution evidence (`POST /api/incidents/{incidentId}/resolutions`) utilizes the same `FileSecurityValidator` controls. The similarity comparison algorithm evaluates spatial proximity and file size consistency between initial citizen evidence and officer resolution evidence without altering the Phase 7 scoring logic.
