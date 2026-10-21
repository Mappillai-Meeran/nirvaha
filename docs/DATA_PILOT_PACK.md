# Nirvaha Data Pilot Pack

Nirvaha should begin as a focused municipal pilot, not a city-wide claim. Start with one municipality and 3 to 5 wards for 90 days.

## The data you need

| Dataset | Why it matters | Owner | Minimum starting size | Refresh |
| --- | --- | --- | --- | --- |
| Ward boundaries | Assign each report to the accountable ward | Local Administration / GIS cell | All pilot wards as GeoJSON polygons | When boundaries change |
| Civic asset register | Route work to the correct team and measure repeat failures | Engineering, sanitation, electricity teams | Asset ID, type, location, owning department | Monthly |
| Historical complaint register | Establish baseline volume, category mix, and normal resolution time | Municipal call centre / grievance cell | 12 months of closed and open records | One-time initial load, then daily |
| Field work and resolution evidence | Verify that a case was actually fixed | Field supervisors / contractors | Before and after photos, action timestamp, work order | Per action |
| Weather observations | Add context to drainage, flood, and road issues | IMD / approved weather source | Daily rainfall by station | Daily |

Do not collect Aadhaar, voter ID, caste, religion, or precise home address unless there is a documented legal basis and a privacy review. Citizen contact details should be separated from the public map and officer analytics.

## Import templates

### `wards.geojson`

Use official boundaries in WGS84 coordinates (`EPSG:4326`). Each feature must include `ward_id`, `ward_number`, `ward_name`, `municipality`, and `geometry_source_url`.

```json
{
  "type": "Feature",
  "properties": {
    "ward_id": "PUD-01",
    "ward_number": "1",
    "ward_name": "Muthialpet West",
    "municipality": "Puducherry Municipality",
    "geometry_source_url": "https://sec.py.gov.in/pondicherry-municipality-ward-maps",
    "verified_on": "2026-09-26"
  },
  "geometry": { "type": "Polygon", "coordinates": [[[79.82, 11.94], [79.83, 11.94], [79.83, 11.95], [79.82, 11.94]]] }
}
```

### `assets.csv`

```csv
asset_id,asset_type,department,ward_id,latitude,longitude,installed_on,status,last_inspected_on
PUD-DRAIN-0001,STORM_DRAIN,Engineering,PUD-01,11.935000,79.830000,2019-06-01,ACTIVE,2026-08-14
```

Allowed `asset_type` values for the first pilot: `STORM_DRAIN`, `ROAD_SEGMENT`, `STREETLIGHT`, `WATER_PIPE`, `WASTE_BIN`.

### `historical_cases.csv`

```csv
external_case_id,reported_at,category,severity,ward_id,latitude,longitude,source_channel,status,assigned_department,assigned_at,resolved_at,resolution_code,verified_at
GRV-2026-000123,2026-07-13T08:25:00+05:30,DRAINAGE,HIGH,PUD-01,11.935000,79.830000,CALL_CENTRE,RESOLVED,Engineering,2026-07-13T10:00:00+05:30,2026-07-15T16:30:00+05:30,CLEANED_DRAIN,2026-07-16T09:00:00+05:30
```

Every imported record needs a source-system identifier. Never use a citizen name or phone number as an identifier.

### `field_actions.csv`

```csv
external_case_id,action_at,action_type,officer_or_vendor_id,work_order_ref,notes,before_evidence_ref,after_evidence_ref,verified_by,verified_at
GRV-2026-000123,2026-07-15T15:30:00+05:30,CLEANED_DRAIN,VENDOR-21,WO-40681,Removed silt and waste,s3://evidence/before.jpg,s3://evidence/after.jpg,OFFICER-17,2026-07-16T09:00:00+05:30
```

## Data quality gate

Reject a row when its category is unknown, coordinates are outside the pilot boundary, a required timestamp is malformed, or a closed case has no resolution code. Flag rather than silently fix records that have a ward mismatch, duplicate external ID, or a resolution earlier than assignment.

## Official starting sources

- [Puducherry State Election Commission ward maps](https://sec.py.gov.in/pondicherry-municipality-ward-maps) list the municipal wards and are the best starting point for creating a reviewed GeoJSON boundary set.
- [Puducherry LAD delimitation records](https://lad.py.gov.in/delimitation-municipal-wards-and-commune-panchayats) provide the authoritative administrative reference.
- [data.gov.in's Puducherry catalogue](https://data.gov.in/state_utes/Puducherry%20%28UT%29) includes relevant civic-facility datasets; inspect each dataset's schema and licence before import.
- [IMD rainfall information](https://mausam.imd.gov.in/responsive/rainfallinformation.php) is appropriate for rain-related context, but historical or operational use must follow the provider's terms.

## 90-day pilot success measure

Set a baseline before launch, then measure: median acknowledgement time, median assignment time, median verified-resolution time, reopening rate, duplicate-report link rate, and percent of cases assigned to the correct ward. Do not use a predictive risk score as a success metric until the platform has enough verified local outcome data.
