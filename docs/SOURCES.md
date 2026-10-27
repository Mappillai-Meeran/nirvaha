# External Data Sources & Asset Provenance

This document lists all third-party and public data sources, APIs, and media assets integrated into the Nirvaha prototype.

---

## 1. Meteorological Telemetry
- **Provider:** [Open-Meteo Weather API](https://open-meteo.com/)
- **Endpoints:** `https://api.open-meteo.com/v1/forecast` & `https://archive-api.open-meteo.com/v1/archive` (ERA5 Reanalysis)
- **Usage:** Ingests precipitation levels (mm), temperature, and weather codes to calculate localized risk adjustments for drainage and flooding complaints.
- **Licensing:** Open-Meteo Non-Commercial / Attribution (CC BY 4.0). Fallback heuristics applied when endpoint is offline.

---

## 2. Demonstration Photographic Assets
- **Asset 1: Before Drain (`nirvaha-frontend/public/demo/before_drain.jpg`)**
  - Source: [Unsplash photo-1544620347-c4fd4a3d5957](https://images.unsplash.com/photo-1544620347-c4fd4a3d5957)
  - Subject: Urban street waterlogging / blocked drain
  - License: Unsplash Free License
- **Asset 2: After Drain (`nirvaha-frontend/public/demo/after_drain.jpg`)**
  - Source: [Unsplash photo-1584467735871-8e85353a8413](https://images.unsplash.com/photo-1584467735871-8e85353a8413)
  - Subject: Remediated municipal storm drain infrastructure
  - License: Unsplash Free License

---

## 3. Geographic & Boundary References
- **Target Urban Local Body:** Puducherry Municipality (Boulevard Town / Wards 5, 8, 12).
- **Coordinates:** Centered at Puducherry Boulevard sector (`11.9350° N, 79.8300° E`).
- **Processing:** Deterministic Haversine spatial proximity algorithm computed entirely within the application runtime without external GIS vendor lock-in.
