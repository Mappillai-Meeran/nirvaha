# VBYLD 2027 — DEMONSTRATION SCRIPT (3–5 MINUTES)

**Demonstration Scenario**: Puducherry Monsoon Drainage Waterlogging  
**Location**: Puducherry Heritage Boulevard Ward 5 (Lat: 11.9350, Long: 79.8300)  

---

## DEMO TIMELINE & STEP-BY-STEP WALKTHROUGH

### 0:00 – 0:45 | Step 1: Multilingual Citizen Reporting
- **Action**: Open Citizen Portal (`/citizen/report`). Select language **Tamil (தமிழ்)** from top language switcher.
- **Narrative**: *"Citizens in Puducherry can report civic problems in their native language. Here, a citizen selects Tamil and reports a storm drain blockage on Heritage Boulevard."*
- **Inputs**:
  - Title: `"சாக்கடை நீர் தேக்கம்"`
  - Description: `"கனமழைக்குப் பிறகு இந்த வீதியில் சாக்கடை நீர் தேங்கி நிற்கிறது"`
  - Category: `வடிகால்` (Drainage)
  - Address: `"Mission Street / Heritage Boulevard, Ward 5"`
- **Submission**: Click **Submit Civic Report**.
- **Observation**: Success screen highlights:
  - **Original Language**: `TA`
  - **Original Voice Preserved**: `"கனமழைக்குப் பிறகு இந்த வீதியில் சாக்கடை நீர் தேங்கி நிற்கிறது"`
  - **Normalized Civic Meaning**: `"[Normalized TA Representation]: Report regarding DRAINAGE issue - \"கனமழைக்குப் பிறகு...\""`

---

### 0:45 – 1:30 | Step 2: Evidence Hashing & Ward Point-In-Polygon Assignment
- **Action**: View Incident Details.
- **Narrative**: *"Nirvaha immediately hashes the uploaded photo evidence using SHA-256 for auditability. Simultaneously, its spatial engine performs point-in-polygon matching against Puducherry's digitized Gazette boundary polygons."*
- **Telemetry Highlights**:
  - **Assigned Ward**: `Puducherry Heritage Boulevard Ward 5` (Confidence: `1.0`).
  - **Policy SLA Rule**: PWD Drainage SLA — `24 Hours Target Resolution`.
  - **IMD Weather Context**: `48mm Heavy Rainfall Observation Attached`.

---

### 1:30 – 2:30 | Step 3: Civic Intelligence & Risk Radar
- **Action**: Switch role to **Officer** and navigate to the **Command Center** (`/command-center`).
- **Narrative**: *"On the Municipal Command Radar, officers see cross-language intelligence. A second report in English and a third in Hindi at the same location have been detected by our 500m Haversine spatial engine and linked to form a corroboration cluster."*
- **Telemetry Highlights**:
  - **Corroboration Score**: `67.5 / 100` (`HIGH` Priority).
  - **Ward Risk Level**: `78 / 100` (`CRITICAL` Risk Level).
  - **Intelligence Signal**: `HOTSPOT` & `EMERGING ISSUE` (150% volume spike detected).

---

### 2:30 – 3:30 | Step 4: Government Action Queue & Decision Recording
- **Action**: Open the **Municipal Action Center Queue** card on the Command Center. Click **Record Officer Decision** on the top recommendation for Incident `#101`.
- **Narrative**: *"Nirvaha generates an actionable recommendation for municipal officers. The officer inspects the original Tamil text alongside the normalized civic meaning, and records an operational decision."*
- **Decision Workflow**:
  - **Recommended Action**: `DISPATCH_DRAINAGE_CREW`
  - **Decision Options**: `ACCEPT` / `MODIFY` / `DEFER` / `REJECT`
  - **Officer Action**: Select `ACCEPT`, click **Submit Decision**.
- **Outcome**: Audit log records decision with timestamp and officer principal signature. Incident state updates to `IN_PROGRESS`.

---

### 3:30 – 4:30 | Step 5: Resolution Evidence Verification & Operational Learning
- **Action**: Officer completes work and uploads resolution photo evidence (`/incidents/101`).
- **Narrative**: *"Before marking the case resolved, Nirvaha runs resolution verification. It calculates image similarity between pre-action and post-cleanup evidence."*
- **Verification Result**:
  - **Verification Score**: `0.88` (`VERIFIED`).
  - **Status**: Updated to `VERIFIED`.
- **Operational Learning Signal**: System generates a systemic learning signal:
  - `REPEATED_WATERLOGGING_CLUSTER`: *"Recommended Strategy: Schedule pre-monsoon storm drain desilting on Heritage Boulevard North."*

---

### 4:30 – 5:00 | Closing Summary
- *"Nirvaha does not stop when a complaint is registered or marked resolved. It closes the loop with evidence, explainable intelligence, officer decision tracking, and verified operational learning."*
