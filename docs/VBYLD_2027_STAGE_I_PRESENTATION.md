# VBYLD 2027 — STAGE-I PRESENTATION MASTER CONTENT

**Project Name**: NIRVAHA — From Report to Resolution  
**Full Title**: Nirvaha: Trust-Aware Civic Intelligence Platform for Verified Issue Resolution  
**Track**: Governance & Civic Technology  
**Team**: Dracarus  

---

## SLIDE 1 — TITLE & BRANDING

### NIRVAHA
**From Report to Resolution**

*Nirvaha: Trust-Aware Civic Intelligence Platform for Verified Issue Resolution*

> **Turning citizen reports into verified, prioritized and actionable civic intelligence.**

- **Track**: Governance & Civic Technology
- **Team**: Dracarus
- **Context**: VBYLD 2027 — Hack for Social Cause & Code for Communities 2.0

---

## SLIDE 2 — THE PROBLEM

### From Complaints to Civic Action

Urban municipal authorities face a major challenge in community-scale problem resolution:

- **Fragmented Citizen Voice**: Citizen complaints arrive in multiple regional languages without unified categorization.
- **Corroboration Gaps**: Multiple citizens report the same storm drain block or road damage independently, clogging queues.
- **Unverified Priority**: Severity is hard to compare objectively without spatial density, SLA targets, and weather context.
- **Hidden Systemic Patterns**: Emerging hotspots and recurring infrastructure failures remain hidden in tabular complaint logs.
- **Unverified Resolutions**: Complaints marked "resolved" lack post-action evidence verification, leading to citizen distrust and reopen loops.

```text
Citizen Complaints ──► Fragmented Cases ──► Hard to Prioritize ──► Hidden Patterns ──► Unverified Resolution
```

> **The missing layer is not another complaint form. It is civic intelligence that connects report → action → verified resolution.**

---

## SLIDE 3 — THE SOLUTION

### Nirvaha — From Report to Resolution

Nirvaha adds an evidence-driven trust and intelligence layer between reporting and resolution.

```text
REPORT ──► TRUST ──► INTELLIGENCE ──► ACTION ──► VERIFICATION ──► LEARNING
```

### Key Platform Capabilities

1. **Multilingual Native Access**: Citizens report in English, Tamil, Hindi, Malayalam, or Telugu. Original voice is preserved while text is normalized to canonical civic categories.
2. **Data Trust & Provenance**: Evidence SHA-256 hashing, MIME checks, boundary ward point-in-polygon assignment, and dataset source tracking (`CITIZEN` vs `DERIVED_FROM_OFFICIAL`).
3. **Civic Intelligence Engine**: 500m Haversine duplicate clustering, multi-factor priority scoring, IMD weather context integration, 6-signal ward risk forecasting, spatial hotspots, and 7-day emerging issue detection.
4. **Government Action Loop**: Action recommendation queue for officers with explicit decision recording (`ACCEPT`, `MODIFY`, `DEFER`, `REJECT`) and required operational rationales.
5. **Verified Closed Loop**: Before/after image similarity resolution verification, automated case reopening for failed verification, and systemic operational learning signals.

---

## SLIDE 4 — TECHNICAL ARCHITECTURE

### Production-Grade Full-Stack Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                      CITIZEN PORTAL                         │
│           React 18 + TailwindCSS + Multilingual UI          │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP REST (JWT / JSON)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 SPRING BOOT 3 (JAVA 21) API                 │
│      Security / RBAC / Rate Limiting / Validation          │
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
│  Incidents • Evidence • Ward Polygons • Signals • Decisions │
└─────────────────────────────────────────────────────────────┘
```

- **Backend**: Java 21, Spring Boot 3, Spring Security, JWT, JPA / Hibernate, PostgreSQL.
- **Frontend**: React 18, Vite, TailwindCSS, Lucide Icons, Leaflet Maps.
- **Verification Baseline**: 192 passed backend tests, 0 failures, production Vite build passed (`dist/`).

---

## SLIDE 5 — WHAT MAKES NIRVAHA DIFFERENT

### Trust + Intelligence + Verification

1. **Trust & Provenance**: Strict data classification (`CITIZEN`, `DERIVED_FROM_OFFICIAL`, `REFERENCE`). No unverified government partnership claims.
2. **Explainable Intelligence**: Every priority score, ward risk score, spatial hotspot, and action recommendation carries human-readable natural language explainability.
3. **Integrity Radar**: Non-accusatory anomaly detection (`ANOMALY DETECTED — REVIEW RECOMMENDED`).
4. **Decision Support**: Government action recommendations empower officers (`ACCEPT`, `MODIFY`, `DEFER`, `REJECT`) without replacing human decision-making.
5. **Verified Resolution & Learning**: Image similarity scoring validates post-cleanup evidence. Systemic learning signals guide future maintenance.

> **Nirvaha closes the loop instead of stopping at complaint registration.**

---

## SLIDE 6 — REAL CIVIC CONTEXT & IMPACT

### Built for Real Community Context

- **Puducherry Municipal Demonstration**: Digitized Gazette boundary ward polygons (Oulgaret Ward 12, Puducherry Heritage Boulevard Ward 5, Muthialpet North Ward 8), IMD weather station observations, and PWD service SLA rules.
- **Multilingual Accessibility**: Seamless context switching across English (`EN`), Tamil (`TA`), Hindi (`HI`), Malayalam (`ML`), and Telugu (`TE`). Original citizen text is always recoverable.
- **Measurable Civic Impact**:
  - Eliminates duplicate officer dispatches via 500m Haversine spatial clustering.
  - Reduces SLA breach risk through weather-aware priority scoring.
  - Guarantees accountability with post-resolution evidence verification.

---

## SLIDE 7 — DEMONSTRATION & CLOSING

### From Report to Resolution

```text
Citizen Native Report ──► Trust & Evidence ──► Intelligence Radar ──► Government Action ──► Verified Resolution ──► Operational Learning
```

> **Nirvaha doesn't replace government decision-making. It gives municipal authorities verifiable evidence and explainable intelligence to make faster, more accountable decisions.**

**NIRVAHA**  
*From Report to Resolution*
