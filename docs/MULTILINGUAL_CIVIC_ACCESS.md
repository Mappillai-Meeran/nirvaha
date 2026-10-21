# NIRVAHA — Multilingual Civic Access Architecture

## Executive Summary

Phase 15 introduces **Multilingual Civic Access & Language Intelligence** to Nirvaha. The platform empowers citizens to report civic infrastructure issues in their native languages (**English `EN`**, **Tamil `TA`**, **Hindi `HI`**, **Malayalam `ML`**, **Telugu `TE`**) while preserving their original voice and mapping inputs into a unified, language-neutral civic representation.

---

## 1. Core Architectural Principle

```text
CITIZEN NATIVE LANGUAGE (TA / HI / ML / TE / EN)
                      ↓
          LANGUAGE & SCRIPT DETECTION
                      ↓
         ORIGINAL TEXT PRESERVED IN DB
                      ↓
     COMMON CIVIC NORMALIZATION & CATEGORIES
                      ↓
     EXISTING CIVIC INTELLIGENCE ENGINE (PHASES 4, 5, 8, 9, 13, 14)
                      ↓
    PROACTIVE GOVERNMENT ACTION & RESOLUTION LOOP
```

**Key Constraint**: The internal civic categories (`DRAINAGE`, `POTHOLE`, `GARBAGE`, `STREETLIGHT`, `WATER_SUPPLY`, `OTHER`) remain strictly canonical and language-neutral in database schemas and intelligence algorithms. UI translation does NOT fragment civic intelligence.

---

## 2. Supported Languages

| Code | Language | Native Name | Script Range | Keyword Normalization Support |
|---|---|---|---|---|
| `EN` | English | English | ASCII / Latin | Full |
| `TA` | Tamil | தமிழ் | `\u0B80`–`\u0BFF` | Full |
| `HI` | Hindi | हिन्दी | `\u0900`–`\u097F` | Full |
| `ML` | Malayalam | മലയാളം | `\u0D00`–`\u0D7F` | Full |
| `TE` | Telugu | తెలుగు | `\u0C00`–`\u0C7F` | Full |

---

## 3. Original Text Preservation & Normalization Model

`Incident` entity is extended with persistent provenance and normalization fields:

- `originalDescription`: Raw submitted text in native citizen script (e.g. `"மழை பெய்தால் இங்கே தண்ணீர் தேங்குகிறது"`).
- `originalLanguage`: ISO language code detected or selected (`TA`).
- `normalizedDescription`: English-normalized civic description for administrative interoperability (e.g. `"[Normalized TA Representation]: Report regarding DRAINAGE issue - \"மழை பெய்தால் இங்கே தண்ணீர் தேங்குகிறது\""`).
- `normalizationStatus`: Status of normalization (`COMPLETED`, `PENDING`, `FAILED`).
- `languageConfidence`: Detection confidence score (`1.0` for explicit select, `>= 0.85` for script detection).
- `languageProvider`: Audit provider ID (`LOCAL_RULES`, `nirvaha-language-v1`).

---

## 4. Local Rule Provider Architecture (Zero Paid API Dependency)

To prevent production outage risks from external translation rate limits or API failures, Nirvaha implements a fallback-resilient **Local Rule Provider** (`LanguageService`):
1. **Script Detection**: Unicode character frequency counting across Indic script ranges.
2. **Deterministic Keyword Normalization**: High-fidelity mapping of native terms (e.g. Tamil `"வடிகால்"`, Hindi `"जल निकासी"`, Malayalam `"ഡ്രെയിനേജ്"`, Telugu `"డ్రైనేజీ"`) directly to `IncidentCategory.DRAINAGE`.
3. **Provenance Auditing**: Model version `nirvaha-language-v1` and provider `LOCAL_RULES` stored per incident.

---

## 5. UI Localization & Context Switching

- **Frontend Dictionary**: `i18n/translations.js` containing complete UI strings, category labels, and status badges across all 5 languages.
- **LanguageContext**: React Context provider managing active locale with `localStorage` persistence (`nirvaha_language`) and graceful fallback to English (`EN`) for missing keys.
- **Header Switcher**: `LanguageSwitcher.jsx` dropdown integrated into top navigation.
- **Command Center Language Radar**: Allows government officers to filter incident maps and queues by citizen submission language (`ALL`, `TA`, `HI`, `ML`, `TE`, `EN`) while viewing original citizen voices alongside normalized civic meanings.

---

## 6. Verification & Test Suite Baseline

- Backend Unit & Security Tests: **192 passed, 0 failed, 0 errors, 0 skipped**.
- Frontend Production Build: **PASS** (`vite build` completed cleanly).
