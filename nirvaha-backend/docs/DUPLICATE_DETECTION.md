# Nirvaha — Phase 4: Explainable Duplicate Detection Architecture

## Overview
Nirvaha's duplicate detection engine provides a deterministic, rule-based mechanism to identify related civic complaints without relying on opaque machine learning models. This guarantees transparency and explainability for civic authorities and competition evaluators.

---

## Architecture & Principles
1. **Explainable & Deterministic:** Every potential match returns machine-readable reasons explaining *why* it was flagged as a duplicate.
2. **Dynamic On-Demand Calculation:** Duplicate matches are calculated on-demand without requiring artificial persistent duplicate tables or premature merging.
3. **No Merging / Destruction:** The engine only highlights related incidents for human officer review; it never deletes or forcibly merges citizen reports.

---

## Scoring Formula & Thresholds

$$\text{duplicateSimilarityScore} = w_{\text{category}} + w_{\text{location}} + w_{\text{time}}$$

### Configurable Weights & Thresholds
| Component | Condition | Weight | Reason Enum |
| :--- | :--- | :--- | :--- |
| **Category** | Candidate `category == target.category` | `0.40` | `SAME_CATEGORY` |
| **Location** | Haversine distance $\le 500\text{ meters}$ | `0.40` | `LOCATION_WITHIN_THRESHOLD` |
| **Time** | $\|t_{\text{candidate}} - t_{\text{target}}\| \le 24\text{ hours}$ (1440 mins) | `0.20` | `TIME_WITHIN_THRESHOLD` |

### Score Interpretation
* **`1.00`** — Same Category + Location $\le 500\text{m}$ + Reported within $24\text{ hours}$
* **`0.80`** — Same Category + Location $\le 500\text{m}$ + Reported outside $24\text{ hours}$
* **`0.00`** — Different category or Location $> 500\text{m}$ (Filtered out)

---

## Haversine Distance Formula
Geographic distance between target coordinates $(lat_1, lon_1)$ and candidate coordinates $(lat_2, lon_2)$ is computed using the Great-Circle Haversine equation:

$$a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1) \cdot \cos(\phi_2) \cdot \sin^2\left(\frac{\Delta \lambda}{2}\right)$$

$$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1-a}\right)$$

$$d = R \cdot c$$

where $R = 6,371,000\text{ meters}$ (Earth radius), $\phi$ is latitude in radians, and $\lambda$ is longitude in radians.

---

## REST API Contract

### Endpoint
`GET /api/incidents/{incidentId}/duplicates`

### Example Response
```json
{
  "incidentId": 46,
  "matches": [
    {
      "incidentId": 47,
      "duplicateSimilarityScore": 1.0,
      "distanceMeters": 15.6,
      "timeDifferenceMinutes": 2,
      "reasons": [
        "SAME_CATEGORY",
        "LOCATION_WITHIN_THRESHOLD",
        "TIME_WITHIN_THRESHOLD"
      ]
    }
  ]
}
```
