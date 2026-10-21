# Nirvaha Frontend Integration & End-to-End Validation Guide

## 1. Overview
This document describes the complete frontend integration connecting **Phases 1–9** of the `nirvaha-backend` into a unified **Nirvaha Civic Intelligence Portal & Municipal Command Center**.

The frontend provides:
1. **Citizen Experience**: Issue reporting, photo evidence attachment, duplicate warning notification, status tracking, and resolution verification badge display.
2. **Municipal Command Center**: Real-time Leaflet spatial heatmap layer, 6-signal ward risk evaluation breakdown, Open-Meteo weather clarity indicator, priority queue matrix, and integrity radar.

---

## 2. End-to-End Intelligence Flow
```text
CITIZEN REPORT 
   ↓
EVIDENCE PHOTO 
   ↓
DUPLICATE CHECK (500m Haversine)
   ↓
COMMAND CENTER (Live Spatial & Ward Risk)
   ↓
PRIORITY EVALUATION (Severity, Recurrence, Clustering)
   ↓
OFFICER WORKFLOW (Assign → Start Field Work)
   ↓
RESOLUTION SUBMISSION (After-Photo Evidence)
   ↓
VISUAL VERIFICATION (Simulated Verification Status)
   ↓
INTEGRITY & ANOMALY RADAR
```

---

## 3. Environment & Setup

### Requirements
* Node.js v18+
* Java 17 / Spring Boot backend (`http://localhost:8080`)

### Environment Configuration
The frontend communicates with the backend via `VITE_API_BASE_URL` defined in `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### Running Locally
```powershell
cd E:\Nirvaha\nirvaha-frontend
npm install
npm run dev
```

### Production Build
```powershell
cd E:\Nirvaha\nirvaha-frontend
npm run build
```

---

## 4. Key Components & Integration Architecture

* **Central API Client** ([`src/services/apiClient.js`](file:///e:/Nirvaha/nirvaha-frontend/src/services/apiClient.js)): Centralizes HTTP requests, JSON error normalization, multipart file upload, and base URL handling.
* **Command Center Map** ([`src/components/map/CommandMap.jsx`](file:///e:/Nirvaha/nirvaha-frontend/src/components/map/CommandMap.jsx)): Leaflet Dark-mode spatial map visualizing incident pins and ward risk boundaries.
* **Weather Clarity Indicator** ([`src/components/ui/index.jsx`](file:///e:/Nirvaha/nirvaha-frontend/src/components/ui/index.jsx)): Explicitly differentiates active live weather (`0mm` rain $\to$ `0.0` impact) from Open-Meteo fail-safe fallback (`Weather Data Unavailable`).
* **Incident Intelligence Detail** ([`src/pages/government/IncidentDetail.jsx`](file:///e:/Nirvaha/nirvaha-frontend/src/pages/government/IncidentDetail.jsx)): Unified single-page view demonstrating the complete intelligence chain for any civic issue.

---

## 5. Security & Authentication Boundary
Role switching between `CITIZEN`, `OFFICER`, and `ADMIN` is implemented via `AuthContext.jsx` for demonstration and hackathon review purposes. Production deployment requires wiring Spring Security JWT headers into `apiClient.js`.
