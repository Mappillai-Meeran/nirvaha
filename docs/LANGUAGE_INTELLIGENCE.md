# NIRVAHA — Cross-Language Civic Intelligence Specification

## 1. Overview

This document specifies how cross-language citizen submissions participate seamlessly in Nirvaha's existing intelligence pipelines without fragmentation or duplicate intelligence models.

---

## 2. Cross-Language Intelligence Matrix

```text
Report A (Tamil): "மழை பெய்தால் இங்கே தண்ணீர் தேங்குகிறது" ─┐
                                                         ├─► Canonical Category: DRAINAGE
Report B (Hindi): "बारिश के बाद पानी भर जाता है"────────┤   Location: Boulevard Ward 5
                                                         │   Timestamp: 2026-09-26
Report C (English): "Waterlogging after rain"────────────┘
                                                         │
                                                         ▼
                                       ┌───────────────────────────────────┐
                                       │ Nirvaha Intelligence Engine       │
                                       ├───────────────────────────────────┤
                                       │ • Duplicate Detection (Phase 4)   │
                                       │ • Priority Intelligence (Phase 5) │
                                       │ • Hotspot Clustering (Phase 13)   │
                                       │ • Emerging Issue Radar (Phase 13) │
                                       │ • Risk Intelligence (Phase 9)     │
                                       │ • Action Recommendations (Ph 14)  │
                                       └───────────────────────────────────┘
```

---

## 3. Service Interactions

### 3.1 Duplicate Detection (Phase 4)
- **Primary Keys**: Canonical `category` (`DRAINAGE`), spatial proximity (Haversine formula within 500m), temporal window (within 72 hours).
- **Result**: Tamil, Hindi, and English reports in the same neighborhood link together into a single spatial cluster.

### 3.2 Spatial Hotspots & Emerging Issues (Phase 13)
- **DBSCAN Clustering**: Operates on `(latitude, longitude, category)` regardless of `original_language`.
- **Emerging Candidate Detection**: Evaluates percentage growth between current 7-day window and previous 7-day window for canonical category `DRAINAGE`.

### 3.3 Priority & Risk Intelligence (Phases 5 & 9)
- **Scoring**: Base severity, corroboration count across languages, weather severity (IMD rainfall adapter), and policy SLA tracking operate on the normalized record.

---

## 4. API Endpoints

- `GET /api/language/supported`: Returns supported language codes, native names, default language, provider, and model version.
- `POST /api/language/detect`: Accepts `{ "text": "...", "preferredLanguage": "TA" }` and returns script detection confidence, canonical category, and normalized civic text.
