# NIRVAHA — MAP CONFIGURATION & TILE SPECIFICATION

## Overview

Nirvaha utilizes **Leaflet GIS** (`leaflet` + `react-leaflet`) for spatial visualization across the Government Command Center and Incident Case Detail screens.

---

## Tile Provider Architecture & Configuration

Leaflet requires a raster/vector tile tile server for basemap rendering. Nirvaha uses standard **OpenStreetMap (OSM)** tiles following official OpenStreetMap Tile Usage Policy.

```javascript
export const MAP_CONFIG = {
  tileUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  maxZoom: 19,
  minZoom: 10,
  defaultCenter: [11.9350, 79.8300], // Puducherry Municipal Center
  defaultZoom: 13,
};
```

---

## Component Abstraction & Fallback Strategy

The map canvas is encapsulated inside [`CommandMap.jsx`](file:///e:/Nirvaha/nirvaha-frontend/src/components/map/CommandMap.jsx).

### Tile Error & Network Resilience
1. **Tile Load Handling**: `tileerror` events on Leaflet TileLayer trigger an automatic fallback state.
2. **Graceful Degradation Panel**: If OpenStreetMap tile servers are unreachable or offline, the UI renders a non-blocking fallback canvas:

```text
┌─────────────────────────────────────────────────────────────┐
│                    MAP TILES UNAVAILABLE                    │
│   Geographic tile server unreachable or network offline.    │
│   Civic intelligence telemetry remains fully operational.   │
│                                                             │
│   [ Retry Tile Load ]                                       │
└─────────────────────────────────────────────────────────────┘
```

3. **No Fake API Keys**: Leaflet + OpenStreetMap requires **zero API keys**. No fake or hardcoded API keys are used.

---

## Map Layers & Overlays

1. **Incidents Layer**: Color-coded markers based on severity (`CRITICAL`: Red, `HIGH`: Amber, `MEDIUM`: Sky, `LOW`: Slate).
2. **Ward Boundaries Layer**: Polygon GeoJSON rendering of Puducherry Gazette Wards (Ward 5, Ward 12, Ward 8) colored by risk level (`CRITICAL`: Red fill, `HIGH`: Amber fill, `LOW`: Green fill).
3. **Hotspot Clusters Layer**: Semi-transparent spatial density circles representing 500m Haversine incident concentrations.
