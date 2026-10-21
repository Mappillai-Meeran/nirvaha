# ACTION RECOMMENDATIONS ENGINE (PHASE 14)

## 1. Overview

The Action Recommendation Engine (`ActionRecommendationService`) evaluates eligible incident reports and pattern signals to generate structured, explainable action recommendations for municipal officers.

Model Version: `nirvaha-action-v1`

---

## 2. Controlled Action Vocabulary

The engine produces recommendations using a controlled vocabulary (`RecommendedActionType`):

| Action Type | Condition / Trigger | Suggested Department |
| :--- | :--- | :--- |
| `REOPEN_CASE` | Previous resolution evidence failed verification check. | Designated Department |
| `REVIEW_ANOMALY` | Open integrity anomaly flag detected on incident. | AUDIT & INTEGRITY |
| `FIELD_INSPECTION` | Critical priority or high ward risk + heavy rainfall context / emerging surge. | PUBLIC WORKS / DRAINAGE |
| `START_REPAIR` | Incident SLA status is `OVERDUE` or `APPROACHING_DEADLINE`. | FIELD MAINTENANCE |
| `REQUEST_MORE_EVIDENCE` | Zero evidence photos attached to incident report. | CITIZEN LIAISON |
| `ASSIGN_DEPARTMENT` | New `REPORTED` incident requiring departmental assignment. | MUNICIPAL OPERATIONS |
| `VERIFY_RESOLUTION` | Incident in `RESOLVED` status awaiting administrative verification. | SUPERVISORY REVIEW |
| `MONITOR` | Incident progressing under standard operational procedures. | CIVIC SERVICES |

---

## 3. Rationale & Explainability

Every recommendation includes:
* `reason`: Explains why Nirvaha generated this recommendation (e.g., *"Resolution evidence failed verification"*).
* `suggestedDepartment`: Recommends the responsible municipal department (e.g., *"PUBLIC WORKS DEPARTMENT (PWD)"*).
* `suggestedNextStep`: Provides actionable guidance for field crews.
* `supportingSignals`: List of underlying telemetry indicators (e.g., `["SLA Status: OVERDUE", "Priority Level: CRITICAL"]`).

---

## 4. Officer Decision Recording

Officers interact with recommendations via the API (`POST /api/action-center/decisions`):

* `ACCEPT`: Officer confirms and executes the recommended action.
* `MODIFY`: Officer alters the course of action (requires mandatory written rationale).
* `DEFER`: Officer postpones action (requires mandatory written rationale).
* `REJECT`: Officer rejects recommendation (requires mandatory written rationale).

Decisions are stored permanently in the `officer_decisions` table with timestamp and officer ID for complete governance auditability.
