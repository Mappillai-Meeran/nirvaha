# NIRVAHA — DESIGN SYSTEM SPECIFICATION

## Visual Identity & Design Philosophy

Nirvaha's visual identity communicates **Trust, Precision, Civic Intelligence, Accountability, Action, and Verification**.

It represents a **warm, mature, civic technology product** that bridges citizens and municipal operational workflows.

It intentionally avoids:
- Generic dark-blue SaaS dashboard templates
- Glassmorphism & neon purple AI glows
- Bouncing cards & bubbly 3D blobs
- Cluttered card grids without hierarchy
- Crypto/gaming HUD dark interfaces

It adopts a **warm off-white foundation, deep forest teal primary elements, terracotta accenting, and calm editorial information hierarchy** built for municipal command centers and accessible public service.

---

## Typography System

Typeface: **Inter** / **IBM Plex Sans** (Primary UI & Monospace Data Labels)

| Scale Level | Class / Styling | Usage |
|---|---|---|
| **Display Header** | `text-2xl font-bold tracking-tight text-[#18201F]` | Command Center primary header |
| **Page Title** | `text-xl font-bold text-[#18201F]` | Screen & case file titles |
| **Section Header** | `text-xs font-semibold uppercase tracking-wider text-[#66716E]` | Section dividers & card labels |
| **Body Primary** | `text-sm text-[#18201F] leading-relaxed` | Descriptions & case notes |
| **Body Secondary** | `text-xs text-[#66716E]` | Explanations & metadata text |
| **Data Label** | `text-[11px] font-mono uppercase text-[#66716E]` | Telemetry keys & field labels |
| **Data Value** | `text-xs font-mono font-semibold text-[#163A36]` | Coordinates, hashes, SLA hours |

---

## Core Color System

| Token Name | Hex Code | Description & Usage |
|---|---|---|
| **Primary Brand** | `#163A36` | Deep Forest Teal — Headers, main nav, primary actions |
| **Secondary Brand** | `#2F6F68` | Muted Civic Teal — Secondary buttons, active states |
| **Background Canvas** | `#F5F4EF` | Warm Off-White — Page canvas foundation |
| **Surface** | `#FFFFFF` | White — Information panels, case files, inputs |
| **Primary Text** | `#18201F` | Dark Charcoal — Headings, body text, readable content |
| **Muted Text** | `#66716E` | Slate Gray — Labels, secondary metadata |
| **Border Subtlety** | `#D9DEDB` | Soft Civic Gray — Restrained borders & dividers |
| **Terracotta Accent** | `#C87941` | Warm Terracotta — Focus highlights, key callouts |

---

## Semantic Palette

| Status / Context | Hex Code | Visual Application |
|---|---|---|
| **Critical** | `#B94A48` | Civic Muted Crimson — Urgent priority, SLA breaches |
| **Warning** | `#C58A32` | Muted Amber Gold — Pending action queue, officer review |
| **Success** | `#3F7D5A` | Forest Green — Verified resolution, clean status |
| **Informational** | `#2F6F68` | Muted Civic Teal — Signals, weather context, GIS overlays |

---

## Component Shape Language

1. **Restrained Geometry**: Subtle `6px` to `10px` radii (`rounded-md` / `rounded-lg`). No bubbly cards.
2. **Thin Subtle Borders**: 1px `#D9DEDB` borders defining distinct functional panels.
3. **Structured Surface Rhythm**: Warm off-white `#F5F4EF` layout with crisp `#FFFFFF` operational surfaces.
4. **Human-Readable Intelligence**: Concise telemetry callouts with provenance attribution without AI jargon.
