# NIRVAHA — From Report to Resolution

**Nirvaha: Trust-Aware Civic Intelligence Platform for Verified Issue Resolution**

Nirvaha is an evidence-driven, trust-aware civic intelligence and municipal action platform designed for verified civic problem resolution at community scale.

Prepared for:
1. **VBYLD 2027 — Hack for Social Cause (Stage-I Submission)**
2. **Code for Communities 2.0**

Track: **Governance & Civic Technology**  
Team: **Dracarus**

---

## 1. What is Nirvaha?

Nirvaha turns fragmented citizen complaints into verified, prioritized, and actionable civic intelligence. Rather than acting as another passive complaint form, Nirvaha adds an evidence-driven trust and intelligence layer between reporting and municipal action.

```text
CITIZEN REPORT ──► TRUST & EVIDENCE ──► CIVIC INTELLIGENCE ──► GOVERNMENT ACTION ──► VERIFIED RESOLUTION ──► SYSTEMIC LEARNING
```

---

## 2. The Problem

Urban municipal authorities face major operational bottlenecks:
- **Fragmented Citizen Voice**: Citizen reports arrive in regional languages without unified categorization.
- **Corroboration Gaps**: Multiple citizens report the same storm drain block independently, clogging queues.
- **Unverified Priority**: Urgency is difficult to compare without spatial density, SLA targets, and weather context.
- **Hidden Systemic Patterns**: Emerging hotspots and recurring infrastructure failures remain hidden in complaint logs.
- **Unverified Resolutions**: Complaints marked "resolved" lack post-action evidence verification, causing citizen distrust.

---

## 3. The Solution

Nirvaha closes the loop from report to verified resolution:
- **Native Multilingual Reporting**: Citizens report in English, Tamil (தமிழ்), Hindi (हिन्दी), Malayalam (മലയാളം), or Telugu (తెలుగు). Original voice is preserved while text is normalized to canonical civic categories.
- **Data Trust & Provenance**: Cryptographic evidence hashing (SHA-256), point-in-polygon ward matching against digitized Gazette boundaries, and dataset classification (`CITIZEN` vs `DERIVED_FROM_OFFICIAL`).
- **Civic Intelligence Radar**: 500m Haversine duplicate clustering, weather-aware priority scoring, 6-signal ward risk forecasting, spatial hotspots, and emerging issue detection.
- **Government Action Queue**: Actionable recommendations for officers with decision recording (`ACCEPT`, `MODIFY`, `DEFER`, `REJECT`) and required operational rationales.
- **Verified Resolution & Learning**: Post-action image similarity checks, automatic reopening for failed verification, and systemic operational learning signals.

---

## 4. Closed-Loop Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                      CITIZEN PORTAL                         │
│       React 18 + TailwindCSS + Multilingual Access          │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP REST (JWT / JSON)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 SPRING BOOT 3 (JAVA 21) API                 │
│       Security / RBAC / Rate Limiting / Validation          │
└──────┬───────────────────────┬───────────────────────┬──────┘
       │                       │                       │
       ▼                       ▼                       ▼
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│ Intelligence │       │ Action Center│       │ Data & Rules │
│   Engine     │       │    Loop      │       │  Provenance  │
└──────┬───────┘       └──────┬───────┘       └──────┬───────┘
       │                      │                      │
       └──────────────────────┼──────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     POSTGRESQL DATABASE                     │
│ Incidents • Evidence • Ward Polygons • Signals • Decisions  │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Technology Stack

- **Backend**: Java 21 LTS, Spring Boot 3.4.x, Spring Security, JWT, JPA / Hibernate, PostgreSQL 16.
- **Frontend**: React 18, Vite 5, TailwindCSS, Lucide React Icons, Leaflet GIS Maps.
- **Build & Quality**: Maven 3.9+, Vite reporter, JUnit 5, MockMvc.

---

## 6. Verification & Build Baseline

```text
Backend Test Suite:   192 passed, 0 failed, 0 errors, 0 skipped (.\mvnw.cmd test)
Frontend Build:       PASS (Vite production bundle built cleanly in 986ms)
Product Status:       OFFICIAL PRODUCT FREEZE — READY FOR SUBMISSION
```

---

## 7. Project Documentation

Comprehensive documentation is available in the [`docs/`](docs/) directory:

- [`docs/JUDGES_QUICK_READ.md`](docs/JUDGES_QUICK_READ.md) — 2-minute overview for competition judges.
- [`docs/VBYLD_2027_STAGE_I_PRESENTATION.md`](docs/VBYLD_2027_STAGE_I_PRESENTATION.md) — 7-slide master presentation content.
- [`docs/VBYLD_2027_DEMO_SCRIPT.md`](docs/VBYLD_2027_DEMO_SCRIPT.md) — 3–5 minute step-by-step demonstration scenario script.
- [`docs/VBYLD_2027_DEMO_DATA.md`](docs/VBYLD_2027_DEMO_DATA.md) — Deterministic scenario dataset specification.
- [`docs/NIRVAHA_ARCHITECTURE.md`](docs/NIRVAHA_ARCHITECTURE.md) — Full-stack architecture specification.
- [`docs/VBYLD_2027_TECHNICAL_SUMMARY.md`](docs/VBYLD_2027_TECHNICAL_SUMMARY.md) — One-page technical summary.
- [`docs/VBYLD_2027_FORM_ANSWERS.md`](docs/VBYLD_2027_FORM_ANSWERS.md) — Master submission form answers draft.
- [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md) — Technical boundaries and limitations.
- [`docs/PHASE_16_COMPETITION_REALITY_AUDIT.md`](docs/PHASE_16_COMPETITION_REALITY_AUDIT.md) — Competition reality audit report.
- [`docs/PRODUCT_FREEZE.md`](docs/PRODUCT_FREEZE.md) — Official Product Freeze declaration.
- [`docs/reports/`](docs/reports/) — Historical phase development logs, security audits, and verification reports (Phases 1 through 5C.5).

---

## 8. Running Locally

### One-Click Startup Script (Windows Batch Launcher)
You can launch both the Spring Boot Backend and React Frontend with a single click:
```cmd
start.bat
```
or
```cmd
run_nirvaha.bat
```

### Local Development Prerequisites
- **Java 21 LTS** and **Maven** (included via `./mvnw` wrapper).
- **Node.js 18+** and **npm**.
- **PostgreSQL 16** running locally with a database named `nirvaha_db`.

### Local Database Configuration
By default, the backend connects to `jdbc:postgresql://localhost:5432/nirvaha_db` with username `postgres` and password `postgres`.
If your local PostgreSQL credentials differ, you can provide them using any of these approaches without modifying tracked files:
- **Environment Variable**: Set `DATABASE_PASSWORD=your_password` in your terminal or environment.
- **Local Properties File**: Create `nirvaha-backend/application-local.properties` (automatically ignored by git):
  ```properties
  spring.datasource.password=your_password
  ```
- **Local .env File**: Copy `.env.example` to `.env` and set `DATABASE_PASSWORD=your_password`.

### Manual Component Startup
**Backend**:
```bash
cd nirvaha-backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```
Backend runs locally on `http://localhost:8080`.

**Frontend**:
```bash
cd nirvaha-frontend
npm install
npm run build
npm run dev
```
Frontend runs locally on `http://localhost:5173`.

---

## 9. Docker Configuration

Dockerfile and Compose configurations are included (`Dockerfile`, `docker-compose.yml`) and verified as valid artifacts. Runtime execution in the local evaluation environment was blocked by local Windows/WSL2 distribution availability; standalone Spring Boot and Vite runtimes execute cleanly.

---

## 10. License

This project is licensed under the [MIT License](LICENSE).
