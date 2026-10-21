# VBYLD 2027 — ONE-PAGE TECHNICAL SUMMARY

## Product Title
**NIRVAHA — From Report to Resolution**  
*Trust-Aware Civic Intelligence Platform for Verified Issue Resolution*

## Domain / Track
**Governance & Civic Technology**

---

## Technical Stack At A Glance

- **Backend Runtime**: Java 21 LTS, Spring Boot 3.4.x, Maven 3.9+.
- **Security**: Spring Security, JWT (JSON Web Tokens), BCrypt Password Hashing, Rate Limiting Filter.
- **Frontend Stack**: React 18, Vite 5, TailwindCSS, Lucide React Icons, Leaflet GIS Maps.
- **Database**: PostgreSQL 16 / H2 spatial fallback schema.
- **Testing Baseline**: 192 passed backend unit/integration tests, 0 failures, 0 errors, 0 skipped.
- **Frontend Build**: PASS (`npm run build` Vite production bundle built in 986ms).

---

## Core Technical Innovations

1. **Multilingual Script Detection & Canonical Normalization**: Citizens report in English (`EN`), Tamil (`TA`), Hindi (`HI`), Malayalam (`ML`), or Telugu (`TE`). Script frequency analysis and keyword normalization map reports into 6 canonical categories (`DRAINAGE`, `POTHOLE`, `GARBAGE`, `STREETLIGHT`, `WATER_SUPPLY`, `OTHER`) while preserving original native text.
2. **500m Haversine Duplicate Clustering**: Identifies cross-language corroborating reports within 500 meters and 72 hours, eliminating duplicate dispatches and strengthening priority scores.
3. **6-Signal Ward Risk Forecasting**: Multi-factor 0-100 risk scoring combining incident density, severity, SLA pressure, weather context, unresolved load, and historical recurrence.
4. **Data Provenance & Trust Classification**: Strict dataset tagging (`CITIZEN`, `DERIVED_FROM_OFFICIAL`, `REFERENCE`, `SIMULATED`). 100% isolation of simulated test data from production risk forecasting.
5. **Government Decision Support Loop**: Action recommendation queue for municipal officers with decision recording (`ACCEPT`, `MODIFY`, `DEFER`, `REJECT`) and required operational rationales.
6. **Verified Resolution & Learning Signals**: Image similarity scoring verifies post-cleanup evidence. Automated case reopening loop handles failed verification. Operational learning signals derive long-term maintenance strategies.

---

## Verification & Build Summary

```text
Backend Tests:  192 passed, 0 failed, 0 errors, 0 skipped (.\mvnw.cmd test)
Frontend Build: PASS (vite build succeeded in 986ms)
Product Status: OFFICIAL PRODUCT FREEZE — READY FOR SUBMISSION
```
