# CLOSED-LOOP LEARNING SIGNALS (PHASE 14)

## 1. Overview

The Learning Signal Engine (`LearningSignalService`) evaluates historical resolution outcomes, reopening rates, SLA service pressure, and recurring spatial concentrations to generate system-wide operational learning signals.

Model Version: `nirvaha-learning-v1`

---

## 2. Learning Signal Categories

| Signal Type | Evaluation Logic | Recommended Operational Strategy |
| :--- | :--- | :--- |
| `ELEVATED_REOPEN_FREQUENCY` | Detects high resolution verification failure rate for specific incident categories. | Review resolution evidence protocols and field repair verification standards for category. |
| `SLA_SERVICE_PRESSURE` | Detects incident categories frequently approaching or exceeding SLA standards. | Evaluate resource allocation and dispatch capacity for maintenance teams. |
| `RECURRING_INFRASTRUCTURE_VULNERABILITY` | Identifies persistent incident recurrence in the same ward across multiple periods. | Initiate preventative structural overhaul rather than repeated reactive patch repairs. |

---

## 3. Data Integrity & Isolation Rules

* **Simulated Data Exclusion**: Incidents with `classificationTag = SIMULATED` are strictly excluded from learning calculations.
* **Non-Surveillance Policy**: Learning signals evaluate system-wide infrastructure and process performance. No individual officer surveillance or worker ranking is performed.
* **Deterministic Output**: Learning signals use rule-based pattern evaluation to ensure reproducible and auditable results.
