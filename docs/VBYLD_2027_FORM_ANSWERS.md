# VBYLD 2027 — MASTER SUBMISSION FORM ANSWERS

> **DRAFT — ADAPT TO OFFICIAL FORM FIELD CHARACTER LIMITS**

---

### 1. Project Title
**Nirvaha: Trust-Aware Civic Intelligence Platform for Verified Issue Resolution**

### 2. Tagline
**From Report to Resolution**

### 3. Problem Statement
Urban municipal authorities face severe challenges in resolving community civic issues. Citizen complaints are fragmented across regional languages without unified categorization, leading to duplicate officer dispatches, unverified priority, hidden spatial hotspots, and unverified resolution claims. Existing grievance forms route complaints but lack the evidence-driven trust and intelligence required to close the loop from report to verified resolution.

### 4. Proposed Solution
Nirvaha is a trust-aware civic intelligence and municipal action platform that connects citizen reports with real civic data, spatial duplicate detection, weather context, ward risk forecasting, officer decision support, resolution evidence verification, and systemic learning signals. Citizens report in their native language (English, Tamil, Hindi, Malayalam, Telugu) while Nirvaha preserves their original voice and maps reports into canonical civic categories for actionable municipal resolution.

### 5. Innovation & Uniqueness
1. **Multilingual Access + Canonical Model**: Preserves native citizen voice while normalizing text into common civic categories across 5 Indic languages.
2. **Data Trust & Provenance**: SHA-256 evidence hashing, point-in-polygon ward assignment against digitized Gazette boundary polygons, and strict dataset classification (`CITIZEN`, `DERIVED_FROM_OFFICIAL`).
3. **Decision Support (Not Replacement)**: Provides municipal officers with actionable recommendations and records explicit officer decisions (`ACCEPT`, `MODIFY`, `DEFER`, `REJECT`) with operational rationales.
4. **Verified Closed Loop**: Image similarity checks validate post-action resolution evidence before closing cases, triggering automated reopening for failed verification and generating systemic learning signals.

### 6. Technology Stack
- **Backend**: Java 21, Spring Boot 3, Spring Security, JWT, JPA/Hibernate, PostgreSQL.
- **Frontend**: React 18, Vite 5, TailwindCSS, Leaflet Maps, Lucide Icons.
- **Testing**: 192 passed unit/integration tests, 0 failures.

### 7. Target Beneficiaries & Social Impact
- **Citizens**: Transparent reporting in native languages, real-time status tracking, and verifiable resolution.
- **Municipal Authorities & Officers**: Reduced duplicate dispatches via 500m spatial clustering, weather-aware priority scoring, clear action queues, and systemic operational learning.

### 8. Implementation Status
Full-stack implementation completed and verified across 16 development phases. Production build passed (`npm run build`). Test suite passed (192 tests). Official Product Freeze in effect.
