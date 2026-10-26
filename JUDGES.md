# NIRVAHA — Evaluator Quick-Start Guide

**Hackathon Evaluation & Demonstration Reference**  
*Track: Governance & Civic Technology | Team: Dracarus*

---

## 1. Quick Startup

Launch both the Spring Boot backend and React frontend concurrently:

```cmd
start.bat
```

| Service | Local URL | Port | Runtime |
| :--- | :--- | :---: | :--- |
| **Backend REST API** | `http://localhost:8080` | `8080` | Spring Boot 4.1.1 (Java 21 LTS) |
| **Frontend Portal** | `http://localhost:5173` | `5173` | React 19 + Vite + Tailwind CSS |

*Prerequisites: PostgreSQL 16+ running on port 5432 with database `nirvaha_db`.*

---

## 2. Verified Demo Credentials

Use the **Demo Quick-Fill** buttons on `http://localhost:5173/login` or enter the verified seed credentials below:

| Role | Email Address | Password | Intended Portal Landing |
| :--- | :--- | :--- | :--- |
| **Citizen** | `citizen@puducherry.gov.in` | `Citizen123!` | `/citizen/report` |
| **Officer** | `officer@puducherry.gov.in` | `Officer123!` | `/command-center` |
| **Admin** | `admin@puducherry.gov.in` | `Admin123!` | `/command-center` |

---

## 3. Four-Step Golden Path Demonstration

Follow this sequence to evaluate the complete reporting-to-verification lifecycle in under 5 minutes:

### Step 1: Citizen Authentication
- Open `http://localhost:5173/login`.
- Click the **Citizen** demo quick-fill button (fills `citizen@puducherry.gov.in` / `Citizen123!`).
- Click **Sign In**. The portal automatically redirects to `/citizen/report`.

### Step 2: Submit Waterlogging Report with Evidence
- Route: `http://localhost:5173/citizen/report`
- Select or enter issue details:
  - **Category:** Waterlogging / Drainage Obstruction
  - **Description:** *"Water overflowing from blocked stormwater canal near Krishna Nagar junction"*
- Click the interactive GIS map to set coordinates in Krishna Nagar (or let GPS default).
- Attach sample photo evidence (JPEG/PNG) and click **Submit Report**.
- **Outcome:** The report is ingested, assigned a tracking ID, stamped with a client/server SHA-256 cryptographic digest, and matched to municipal ward boundaries.

### Step 3: Officer Triage & Priority Queue Inspection
- Log out via the header profile menu and return to `http://localhost:5173/login`.
- Click the **Officer** demo quick-fill button (`officer@puducherry.gov.in` / `Officer123!`) and click **Sign In**.
- Navigate to **Priority Queue** (`http://localhost:5173/command-center/queue`):
  - Inspect the calculated explainable priority score ($P \in [0.0, 1.0]$).
  - Observe explanation chips (e.g., rainfall context, spatial cluster density).
- *(Optional)* Navigate to **Integrity Radar** (`http://localhost:5173/command-center/anomalies`) to inspect 500m spatial duplicate clusters.

### Step 4: Accept Incident & Submit Resolution Proof
- In the Priority Queue card, click **Accept Recommendation** and enter an operational rationale (e.g., *"Dispatched PWD Desilting Crew 3"*).
- Click the incident card or navigate to `http://localhost:5173/incidents/1` to view incident details.
- Under the Resolution section, upload post-action completion evidence (photo of cleared drain).
- **Workflow State:** The resolution evidence is hashed with SHA-256 and compared against pre-action report evidence. The case transitions to resolution review, initiating the 72-hour citizen dispute window before permanent closure.

---

## 4. Key Active Application Routes

- **Citizen Reporting:** `/citizen/report`
- **Citizen My Reports:** `/citizen/my-reports`
- **Government Command Center:** `/command-center`
- **Municipal Priority Queue:** `/command-center/queue`
- **Duplicate & Integrity Radar:** `/command-center/anomalies`
- **Incident Detail & Resolution Proof:** `/incidents/:id`
