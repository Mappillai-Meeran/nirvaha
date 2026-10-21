# Nirvaha Showcase Runbook

This is the fastest credible way to show Nirvaha working end to end without presenting generated cases as live government records.

## Use three clearly labelled data lanes

| Lane | Use in the showcase | Source | Label in product |
| --- | --- | --- | --- |
| Public reference data | Ward boundaries, streets, landmarks, rain context | Official ward maps, OpenStreetMap, IMD | `Reference data` |
| Partner operational data | Historical cases, routing, work orders, verified resolutions | Municipality or Local Administration Department export | `Municipal pilot data` |
| Live pilot reports | New reports submitted by invited residents and field staff | Nirvaha citizen app | `Live pilot report` |

Never mix these labels. A dashboard case must show whether it is simulated, imported from a partner, or submitted during the pilot.

## Where to get the real data

1. **Ward boundaries:** start with the Puducherry State Election Commission ward maps. Request the GIS/vector boundary file from the municipality or digitise a small, reviewed set of three wards from the official maps into `wards.geojson`. Do not infer ward boundaries from points alone.
   - https://sec.py.gov.in/pondicherry-municipality-ward-maps
   - https://sec.py.gov.in/ward-maps

2. **Roads, drains, public features and local context:** use OpenStreetMap as base reference data. It gives a realistic map for a demo, but must not be treated as the municipal asset register. Keep the OpenStreetMap attribution visible and verify critical assets in the field.
   - https://www.openstreetmap.org/copyright

3. **Historical municipal complaints:** ask the Puducherry Municipality or Local Administration Department grievance cell for a read-only CSV export covering the last 6-12 months for three pilot wards. Ask for case ID, timestamps, category, ward, coordinates or address/landmark, department, status, work-order number, and resolved timestamp. Remove name, phone number, exact home address, and original photos before import.
   - https://lad.py.gov.in/rti-disclosures
   - https://lad.py.gov.in/sites/default/files/Redressal.pdf

4. **Existing grievance systems:** Puducherry already directs residents to grievance portals such as the LG Petition Monitoring System and CPGRAMS. Do not scrape citizen cases from them. Request an authorised aggregate report or export through the responsible department, and document the legal basis and refresh schedule.
   - https://lgredressal.py.gov.in/pgrs/
   - https://pgportal.gov.in/

5. **Rainfall context:** use IMD rainfall information to explain drainage/flood clustering. It is context only; do not claim it proves a road or drainage fault.
   - https://mausam.imd.gov.in/responsive/rainfallinformation.php

## The minimum partner export to request

Ask for this CSV once for the demo, then daily during the pilot:

```text
external_case_id,reported_at,category,ward_id,latitude,longitude,source_channel,
status,assigned_department,assigned_at,resolved_at,resolution_code,work_order_ref
```

Use [DATA_PILOT_PACK.md](DATA_PILOT_PACK.md) for the import schema and quality rules. A source-system ID is mandatory; citizen name and phone number are not.

## A strong 15-minute showcase

1. Start in the citizen experience. Select Tamil, Hindi, Malayalam, Telugu, or English; submit a new issue with a landmark, map pin, and a camera photo.
2. Show duplicate protection: use the same photo twice and explain that identical files are blocked, while visually similar-photo detection remains a future, privacy-reviewed capability.
3. Move to the Command Center. Show the incoming case, ward assignment, category routing, and the clearly labelled reference or municipal-pilot data layer.
4. Assign the case to the responsible department and create a field-work action.
5. From the field workflow, upload before/after evidence and record a resolution.
6. Return to the citizen case timeline and show the resident confirming or reopening the result.
7. End with the operational dashboard: acknowledgement time, assignment time, verified resolution time, duplicate-link rate, and reopen rate.

## Three-week route to a credible pilot

| Week | Outcome | Owner |
| --- | --- | --- |
| 1 | Choose three wards, obtain GIS boundary approval, appoint category owners | Municipality + product lead |
| 2 | Clean and import 6-12 months of de-identified case history; agree status and category mapping | Grievance cell + data lead |
| 3 | Run 20-50 invited reports with field staff; validate routing, evidence flow, and the five operational measures | Citizen support + field supervisors |

## Do not claim these yet

- City-wide coverage
- Live government integration, unless an authenticated export/API is running
- AI fraud or duplicate detection beyond identical-file hashing
- Resolution performance that includes generated or unverified demo cases

The practical pitch is: **Nirvaha is ready for a three-ward, 90-day municipal pilot using official boundaries, authorised complaint history, and consented live reports.**
