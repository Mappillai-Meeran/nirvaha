# NIRVAHA GOVERNMENT ACTION & CLOSED-LOOP OPERATIONAL SYSTEM (PHASE 14)

## 1. Executive Overview

Phase 14 closes the operational loop in Nirvaha by transforming civic intelligence into actionable, auditable government decision-support.

The complete operational loop functions as follows:

```text
CIVIC SIGNAL (Incident Report / Sensor / Feeds)
     ↓
INTELLIGENCE (Hotspot / Trend / Surge / Anomaly)
     ↓
ACTION RECOMMENDATION (Deterministic, Explainable Suggestion)
     ↓
OFFICER DECISION (Accept / Modify / Defer / Reject with Rationale)
     ↓
WORKFLOW ACTION (Assigned / Started / In Progress)
     ↓
RESOLUTION SUBMISSION (Officer Evidence)
     ↓
VERIFICATION (Visual Similarity & Proof Check)
     ↓
OUTCOME (Verified / Failed / Reopened)
     ↓
CLOSED-LOOP LEARNING (Pattern Identification & Strategy Recommendation)
     ↓
FUTURE CIVIC INTELLIGENCE
```

---

## 2. Core Governance Principles

1. **Human-in-the-Loop Decision Authority**: Nirvaha does NOT automatically execute government decisions. The system presents evidence, risk scores, priority signals, and explainable recommendations. The municipal officer remains the authoritative decision-maker.
2. **Accountability & Audit Trail**: Whenever an officer modifies, defers, or rejects a system recommendation, an explicit operational reason is required and recorded in the audit log (`OfficerDecision`).
3. **Reopen Loop**: Resolutions that fail evidence verification automatically generate a `REOPEN_CASE` recommendation.
4. **Simulated Data Exclusion**: `SIMULATED` test fixtures are strictly excluded from production recommendations and learning signals.

---

## 3. Closed-Loop Operational Chain API

The closed-loop operational history for any incident can be retrieved via:

```text
GET /api/action-center/incidents/{incidentId}/full-chain
```

Returns:
* Incident Details
* Evidence Records
* Priority Factors & Risk Scores
* Intelligence Signals (Hotspots, Trends)
* Action Recommendation (`ActionRecommendationDTO`)
* Officer Decisions (`List<OfficerDecisionResponse>`)
* Workflow History (`List<IncidentActionResponse>`)
* Resolution Submission & Verification Result
* Applicable Learning Signals

---

## 4. API Security & RBAC

All `/api/action-center/**` endpoints enforce Spring Security role bindings:
* `OFFICER` and `ADMIN` roles: Granted full access to Municipal Action Center, recommendations, decision submission, and learning signals.
* `CITIZEN` role: Restricted from government action queues and decision recording (returns HTTP 403 Forbidden).
* Unauthenticated calls: Rejected with HTTP 401 Unauthorized.
