# NIRVAHA — FULL-STACK ARCHITECTURE SPECIFICATION

## System Architecture Overview

Nirvaha is structured as a production-ready full-stack civic intelligence platform, decoupling citizen-facing multilingual interfaces from core security, intelligence orchestration, geospatial analysis, decision support, and data provenance.

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                             │
│                  React 18 • Vite • TailwindCSS • Leaflet                │
│       Citizen Multilingual Portal  │  Government Command Center Radar    │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ HTTPS / JWT Bearer Tokens
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        API & SECURITY LAYER                             │
│               Spring Boot 3 Security • JwtAuthenticationFilter          │
│            RateLimitingFilter • Global Exception Sanitizer              │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
           ┌─────────────────────────┼─────────────────────────┐
           ▼                         ▼                         ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│ CIVIC INTELLIGENCE  │   │  MUNICIPAL ACTION   │   │  DATA PROVENANCE    │
│      ENGINE         │   │      CENTER         │   │      REGISTRY       │
│ • Script Detect TA/ │   │ • Recommendation    │   │ • Gazette Polygons  │
│   HI/ML/TE/EN       │   │   Queue Engine      │   │ • IMD Weather AWS   │
│ • Haversine 500m    │   │ • Officer Decision  │   │ • PWD SLA Rules     │
│   Duplicate Cluster │   │   Record (ACCEPT/   │   │ • OpenStreetMap GIS │
│ • 6-Signal Ward Risk│   │   MODIFY/DEFER/     │   │ • Data Classification│
│ • DBSCAN Hotspots   │   │   REJECT)           │   │   (CITIZEN/DERIVED/ │
│ • 7-Day Emerging    │   │ • Resolution Check  │   │   SIMULATED)        │
│   Issue Radar       │   │ • Reopen Loop       │   │                     │
│ • Integrity Radar   │   │ • Learning Signals  │   │                     │
└──────────┬──────────┘   └──────────┬──────────┘   └──────────┬──────────┘
           │                         │                         │
           └─────────────────────────┼─────────────────────────┘
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           DATA STORAGE LAYER                            │
│                       PostgreSQL / Spatial Indexing                     │
│    Incidents • Evidence • Ward Polygons • Signals • Decisions • Rules   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Technical Component Breakdown

### 1. Presentation Layer (Frontend)
- **Framework**: React 18, Vite 5 build tool.
- **Styling**: Vanilla CSS + TailwindCSS (vibrant dark-mode glassmorphism aesthetic).
- **Internationalization**: `LanguageContext.jsx` supporting English (`EN`), Tamil (`TA`), Hindi (`HI`), Malayalam (`ML`), and Telugu (`TE`) with auto-fallback to English.
- **Map Visualization**: Leaflet Maps integration rendering incident locations, ward polygons, and spatial hotspots.

### 2. Security & API Layer (Backend)
- **Framework**: Java 21, Spring Boot 3.4.x, Spring Security.
- **Authentication**: Stateless JWT token authentication with BCrypt password hashing (10 rounds).
- **Authorization**: Role-Based Access Control (`ROLE_CITIZEN`, `ROLE_OFFICER`, `ROLE_ADMIN`).
- **Resilience**: RateLimitingFilter (60 requests/min/IP bucket) and GlobalExceptionHandler returning sanitized error DTOs without stack trace leaks.

### 3. Civic Intelligence Engine
- **Language & Script Detection**: `LanguageService` (Unicode character frequency analysis across Indic scripts + keyword normalization to 6 canonical categories).
- **Spatial Duplicate Clustering**: `DuplicateDetectionService` (Haversine formula within 500m radius and 72h window).
- **Priority Scoring Engine**: Multi-factor scoring (Base Severity + Corroboration + SLA Target + IMD Rainfall Context).
- **Risk Intelligence**: 6-signal ward risk scoring algorithm (0-100 scale).
- **Pattern Radar**: `CivicIntelligenceOrchestrator` (DBSCAN spatial hotspots, 7-day volume spike emerging issue detection, recurring problem identification).
- **Integrity Radar**: Non-accusatory anomaly detection (`ANOMALY DETECTED — REVIEW RECOMMENDED`).

### 4. Municipal Action Center & Closed Loop
- **Action Recommendation Engine**: `ActionRecommendationService` (generates prioritized department actions and next steps).
- **Officer Decision Recording**: `OfficerDecisionService` (persists officer choices: `ACCEPT`, `MODIFY`, `DEFER`, `REJECT` with mandatory operational rationale).
- **Resolution Verification**: `ResolutionVerificationService` (image similarity evaluation between pre-action and post-action evidence).
- **Closed-Loop Learning**: `LearningSignalService` (derives systemic operational strategy insights from historical resolution outcomes and SLA pressure).

### 5. Data Provenance & Trust Layer
- **Source Registry**: `ProvenanceService` (tracks dataset metadata, publisher URLs, licensing, and update timestamps).
- **Geospatial Point-In-Polygon**: `AdministrativeAreaService` (assigns incidents to official Gazette ward polygons; assigns `UNASSIGNED` if outside boundaries).
- **Data Classification**: Strict tagging (`CITIZEN`, `DERIVED_FROM_OFFICIAL`, `REFERENCE`, `SIMULATED`). 100% isolation of `SIMULATED` test records from production intelligence.
