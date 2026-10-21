# REAL DATA SETUP & OPERATIONAL GUIDE — NIRVAHA PHASE 12

## Initial Data Seeding

Upon application startup (`ApplicationReadyEvent`), Nirvaha automatically initializes:

1. **Data Source Registry**: Seeds 6 official data sources (`LAD`, `IMD`, `PWD`, `OGD`, `OSM`, `Nirvaha Citizen`).
2. **Administrative Areas**: Loads verified Puducherry ward boundary polygons with `DERIVED_FROM_OFFICIAL` geometry status.
3. **IMD Rainfall Observations**: Ingests historical daily rainfall readings for Puducherry District (Station 43331).
4. **Service Rules**: Registers PWD Citizen's Charter 15-day urban drainage maintenance policy rule.

---

## Verifying Endpoints

### 1. Data Source Discovery
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/provenance/sources
```

### 2. Administrative Areas & Ward Boundaries
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/provenance/areas
```

### 3. IMD Historical Weather Observations
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/provenance/weather
```

### 4. Policy Service Rules
```bash
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8080/api/provenance/rules
```

---

## Running Verification Suites

### Backend Unit & Integration Tests
```powershell
cd nirvaha-backend
.\mvnw.cmd test
```

### Frontend Production Build
```powershell
cd nirvaha-frontend
npm run build
```
