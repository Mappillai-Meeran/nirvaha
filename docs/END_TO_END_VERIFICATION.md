# NIRVAHA — End-to-End Closed Loop Verification Specification

## Executive Summary

This document verifies the end-to-end execution of Nirvaha's complete closed loop across all 15 completed phases. It details how citizen input flows through evidence ingestion, data trust validation, spatial-temporal duplicate detection, priority scoring, risk prediction, civic intelligence pattern aggregation, municipal action recommendations, officer decisions, resolution verification, and systemic learning.

---

## 1. End-to-End Closed Loop Architectural Flow

```text
               CITIZEN MULTILINGUAL REPORT (EN / TA / HI / ML / TE)
                                       ↓
                        ORIGINAL TEXT PRESERVED IN DB
                                       ↓
                    COMMON CIVIC NORMALIZATION & CATEGORIES
                                       ↓
                 EVIDENCE MANAGEMENT & HASHING (PHASE 3)
                                       ↓
               TRUST & DATA ELIGIBILITY VERIFICATION (PHASE 13)
                                       ↓
             DUPLICATE DETECTION & CLUSTER LINKING (PHASE 4)
                                       ↓
          PRIORITY INTELLIGENCE & WEATHER SCORING (PHASES 5 & 12)
                                       ↓
           6-SIGNAL WARD RISK FORECASTING & HOTSPOTS (PHASES 9 & 13)
                                       ↓
             ACTION RECOMMENDATIONS & QUEUE AUDIT (PHASE 14)
                                       ↓
             OFFICER DECISION RECORDING (ACCEPT/MODIFY/DEFER/REJECT)
                                       ↓
                GOVERNMENT ACTION WORKFLOW & ASSIGNMENT (PHASE 6)
                                       ↓
               RESOLUTION EVIDENCE & SIMILARITY CHECK (PHASE 7)
                                       ↓
            CASE REOPENING LOOP vs VERIFIED RESOLUTION (PHASE 7/14)
                                       ↓
               CLOSED-LOOP SYSTEMIC LEARNING SIGNALS (PHASE 14)
```

---

## 2. Realistic Scenario Walkthrough: Waterlogging on Puducherry Heritage Boulevard

### Step 1: Citizen Multilingual Reporting
- **Citizen Input**: A citizen in Puducherry Heritage Boulevard Ward 5 selects Tamil and submits:
  - `originalDescription`: `"கனமழைக்குப் பிறகு இந்த வீதியில் சாக்கடை நீர் தேங்கி நிற்கிறது"`
  - `originalLanguage`: `"TA"`
  - `latitude`: `11.9350`, `longitude`: `79.8300`
- **Nirvaha Processing**:
  - Script Detection: Identifies Tamil script (`TA`) with 0.92 confidence (`nirvaha-language-v1`).
  - Canonical Mapping: Maps terms `"சாக்கடை நீர்"` and `"தேங்கி"` to canonical category `DRAINAGE`.
  - Normalization: Generates `normalizedDescription`: `"[Normalized TA Representation]: Report regarding DRAINAGE issue - \"கனமழைக்குப் பிறகு இந்த வீதியில் சாக்கடை நீர் தேங்கி நிற்கிறது\""`.

### Step 2: Evidence Management & Hash Ingestion
- Citizen attaches photo evidence of standing water.
- File uploaded to `/api/evidence/incident/{id}`. SHA-256 hash, MIME type `image/jpeg`, and GPS metadata recorded.

### Step 3: Ward Point-in-Polygon & Provenance Attachment
- `AdministrativeAreaService` executes point-in-polygon logic against digitized Gazette boundary polygons.
- Incident assigned to `Puducherry Heritage Boulevard Ward 5` (Method: `POINT_IN_POLYGON`, Confidence: `1.0`).
- Policy SLA Attached: PWD SLA rule for `DRAINAGE` (24-hour target resolution).
- IMD Weather Adapter: Attaches 48mm heavy rainfall observation context from Puducherry Airport AWS station.

### Step 4: Duplicate Detection & Spatial Corroboration
- `DuplicateDetectionService` checks radius within 500 meters and 72-hour window.
- Detects 2 corroborating citizen reports nearby in English and Hindi. Links corroboration flag (`corroborationCount = 3`).

### Step 5: Priority Scoring & Ward Risk Intelligence
- Base Severity: `HIGH` (30 points).
- Corroboration Boost: 3 linked reports (+15 points).
- Rainfall Multiplier: Heavy rainfall context (+12.5 points).
- SLA Pressure: 24h target (+10 points).
- Total Score: `67.5 / 100` (`HIGH` Priority).
- Ward Risk Overview: Ward 5 risk score updated to `78/100` (`CRITICAL` Risk level).

### Step 6: Civic Intelligence Signal Generation
- `CivicIntelligenceOrchestrator` aggregates signals:
  - `HOTSPOT`: Spatial concentration of 3 drainage reports within 200m.
  - `EMERGING_ISSUE`: 150% volume spike in Ward 5 over the past 7 days.

### Step 7: Action Recommendation Engine & Officer Decision
- `ActionRecommendationService` generates recommendation:
  - `recommendedAction`: `DISPATCH_DRAINAGE_CREW`
  - `suggestedDepartment`: `PUDUCHERRY_MUNICIPAL_WORKS`
  - `suggestedNextStep`: `Deploy high-capacity dewatering pump to Heritage Boulevard Ward 5`
  - `priorityLevel`: `CRITICAL`
- Officer in Government Command Center inspects recommendation, reviews original Tamil text and normalized meaning, and records decision:
  - `decision`: `ACCEPT`
  - `decisionId`: Recorded with officer timestamp and principal signature.

### Step 8: Government Workflow & Resolution Verification
- Officer transitions status: `REPORTED` → `IN_PROGRESS` → `RESOLVED`.
- Officer uploads post-cleanup resolution photo.
- `ResolutionVerificationService` runs similarity evaluation:
  - Similarity score: `0.88` (`VERIFIED`).
  - Status updated to `VERIFIED`.

### Step 9: Operational Learning Signal
- `LearningSignalService` evaluates completed resolution chain:
  - Generates `REPEATED_WATERLOGGING_CLUSTER` learning signal.
  - Recommended Operational Strategy: `"Schedule pre-monsoon storm drain desilting on Heritage Boulevard North."`

---

## 3. Verification Summary

The complete closed loop executes asynchronously and end-to-end without data loss, artificial delays, or security breaches.
