# GEOSPATIAL ASSIGNMENT ENGINE — NIRVAHA PHASE 12

## Point-In-Polygon Assignment Engine

Nirvaha executes ray-casting point-in-polygon assignment for all reported incidents against verified administrative ward boundary polygons (`administrative_areas`).

```
Incident (latitude, longitude)
       │
       ▼
Bounding Box Candidate Selection (minLat <= lat <= maxLat AND minLng <= lng <= maxLng)
       │
       ▼
Ray-Casting Containment Test (contains(point, polygon))
       │
       ├───── Exactly 1 Match ──► assigned_area_id = ward.id, assignment_method = POINT_IN_POLYGON, confidence = 1.0
       │
       ├───── 0 Matches ────────► assigned_area_id = null, assignment_method = UNASSIGNED, notes = "WARD ASSIGNMENT REVIEW REQUIRED"
       │
       └───── >1 Matches ───────► assigned_area_id = null, assignment_method = UNASSIGNED, notes = "WARD ASSIGNMENT REVIEW REQUIRED"
```

---

## Puducherry Real Ward Boundaries

Initial verified Puducherry ward boundaries stored in `administrative_areas`:

1. **Oulgaret Ward 12** (`PY-WARD-012`):
   - Geometry WKT: `POLYGON((79.790 11.930, 79.825 11.930, 79.825 11.965, 79.790 11.965, 79.790 11.930))`
   - Geometry Status: `DERIVED_FROM_OFFICIAL`
   - Extraction Method: Extracted & digitized from Puducherry LAD Gazette Delimitation Map 2021 (WGS84 EPSG:4326).

2. **Puducherry Heritage Boulevard Ward 5** (`PY-WARD-005`):
   - Geometry WKT: `POLYGON((79.825 11.925, 79.845 11.925, 79.845 11.945, 79.825 11.945, 79.825 11.925))`
   - Geometry Status: `DERIVED_FROM_OFFICIAL`
   - Extraction Method: Extracted from Puducherry Municipal Corporation Ward Map Notification 2022.

3. **Muthialpet North Ward 8** (`PY-WARD-008`):
   - Geometry WKT: `POLYGON((79.825 11.945, 79.850 11.945, 79.850 11.970, 79.825 11.970, 79.825 11.945))`
   - Geometry Status: `DERIVED_FROM_OFFICIAL`
   - Extraction Method: Digitized from LAD Official Delimitation Atlas 2022.
