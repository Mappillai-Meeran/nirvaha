# DATA SOURCE REGISTRY — NIRVAHA PHASE 12

## Seeded & Consumed Data Sources

Nirvaha maintains a persisted registry of external datasets in `data_sources`. Only sources that Nirvaha actually consumes are registered.

| Source Name | Provider | Source Type | Dataset / URL | Update Freq | License |
|-------------|----------|-------------|---------------|-------------|---------|
| Puducherry Local Administration Department (LAD) | Puducherry LAD | `OFFICIAL_GIS` | Puducherry Ward Delimitation & Maps Catalog (`https://lad.py.gov.in/maps`) | MONTHLY | Official Public Domain / Govt. of Puducherry |
| India Open Government Data Platform (OGD) | NIC / OGD India | `OGD_DATASET` | Puducherry Civic Infrastructure Datasets (`https://data.gov.in/catalog/puducherry-civic-infrastructure`) | QUARTERLY | Open Government Data License - India |
| India Meteorological Department (IMD) | MoES / IMD | `OFFICIAL_WEATHER` | Puducherry District Daily Rainfall & Weather Observations (`https://mausam.imd.gov.in/`) | DAILY | Government of India Public Data |
| Puducherry Public Works Department (PWD) | Govt. of Puducherry PWD | `OFFICIAL_POLICY` | PWD Citizen's Charter 2024 (`https://pwd.py.gov.in/citizens-charter`) | YEARLY | Government Policy Document |
| OpenStreetMap Contributors | OSM Foundation | `OPEN_GEOGRAPHIC` | Puducherry Basemap & Road Network (`https://www.openstreetmap.org`) | CONTINUOUS | Open Database License (ODbL) 1.0 - © OpenStreetMap contributors |
| Nirvaha Citizen Evidence Network | Nirvaha Platform | `NIRVAHA_CITIZEN` | Nirvaha Citizen-Reported Incidents & Verification Evidence (`https://nirvaha.gov.in`) | REALTIME | Nirvaha Platform Terms of Service |

---

## Provenance API Endpoint

- `GET /api/provenance/sources`: Discovers all registered data sources with freshness computation.
- `GET /api/provenance/sources/{id}`: Returns source details and license metadata.
- `GET /api/provenance/areas`: Discovers administrative areas and boundary geometry status.
- `GET /api/provenance/weather`: Returns recent historical IMD weather observations.
- `GET /api/provenance/rules`: Returns registered PWD policy rules and targets.
