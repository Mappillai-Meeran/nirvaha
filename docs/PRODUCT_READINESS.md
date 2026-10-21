# Nirvaha Product Readiness

## Current truth

Nirvaha is a strong civic-operations prototype. It is not yet a production municipal deployment because its data integrations are seeded or reference-based, ward coverage is partial, and the user experience still includes a presentation-only role preview.

## What is already real

- Citizen issue submission with evidence upload, geolocation, multilingual text capture, duplicate checks, workflow actions, and resolution records.
- Role-based API protection, rate limiting, signed authentication, and a production configuration profile.
- A Leaflet map with OpenStreetMap basemap tiles, ward risk overlays, incident markers, and pattern signals.

## Release blockers

| Priority | Gap | Why it blocks a real launch | Owner |
| --- | --- | --- | --- |
| P0 | Official full-ward GeoJSON is not loaded | Reports outside the three digitized wards cannot be reliably assigned | Municipal GIS cell |
| P0 | No signed data-sharing agreement | Complaint history, work orders, and field evidence cannot be treated as operational data without one | Municipality and legal/privacy owner |
| P0 | Presentation role switching remains in the frontend | A real user journey must come from authenticated accounts and assigned roles, not a UI toggle | Product and engineering |
| P0 | Evidence is stored on the local filesystem | Production needs private object storage, malware scanning, retention rules, and access logging | Infrastructure and security |
| P1 | Weather feed is seeded historical context | A production risk feature needs a monitored, licensed ingestion job with freshness alerts | Data engineering |
| P1 | No migration framework | Production schema changes should be versioned with Flyway or Liquibase, not automatic Hibernate updates | Backend engineering |
| P1 | No operational notifications | Officers and citizens need acknowledgement, assignment, due-date, and resolution notifications | Product and operations |
| P1 | Translation is interface-level and rule-based | Test normalisation quality with native speakers and add an escalation path for low-confidence classifications | Language and operations |
| P1 | Evidence deduplication is exact-file only | SHA-256 stops identical uploads; detecting visually similar photos needs a privacy-reviewed perceptual matching policy | Data and security |
| P1 | Camera evidence needs explicit consent | Browser camera access is requested only after a citizen chooses it; publish a clear evidence and retention notice before public launch | Product and legal/privacy owner |

## Decisions you need to make

1. Pick the pilot geography: start with Puducherry Municipality and 3 to 5 wards.
2. Name one accountable municipal owner for every category: drainage, roads, sanitation, streetlights, and water supply.
3. Agree the service promise for each category: acknowledgement, assignment, and verified-resolution time.
4. Decide the citizen identity model: anonymous-with-phone verification, account-based, or integration with an approved government identity system. Do not add identity documents by default.
5. Choose a notification channel: SMS, WhatsApp Business, email, or in-app only. Obtain consent before sending messages.
6. Define evidence retention: who can view photos, how long they are retained, and how citizens can request deletion.

## Product roadmap

### Pilot: 0 to 30 days

Load verified ward GeoJSON, create official department routing rules, import 12 months of cleaned historical cases, and run the product with internal municipal staff only. Keep risk scoring labelled as decision support.

### Controlled public launch: 31 to 90 days

Enable citizen accounts, acknowledgement notifications, an individual case timeline, field-team mobile updates, and post-resolution citizen confirmation. Measure turnaround and reopening before expanding to more wards.

### Scale: after a verified pilot

Integrate municipal work-order systems, replace demo roles, move evidence to managed storage, establish an audited data pipeline, and publish a transparent service-performance dashboard with aggregated data only.
