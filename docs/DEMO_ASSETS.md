# Nirvaha Demo Assets & Evidence Provenance

This document records the provenance, licensing, and cryptographic verification metadata for photographic assets utilized during evaluator demonstrations and automated verification testbeds.

---

## 1. Demonstrated Evidence Artifacts

| Asset Path | Target Usage | Visual Subject | Source URL | Author / Contributor | License | File Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `nirvaha-frontend/public/demo/before_drain.jpg` | Citizen Ingest Evidence (Before Repair) | Urban street waterlogging & storm drain blockage | [Unsplash photo-1544620347-c4fd4a3d5957](https://images.unsplash.com/photo-1544620347-c4fd4a3d5957) | Unsplash Open Library | Unsplash Free License (Commercial & Non-Commercial, CC0 compatible) | ~64 KB | `93734373062566157e4b9606c3c76db2c1c5bc6214d8e2353c35e821c82f9d8a` |
| `nirvaha-frontend/public/demo/after_drain.jpg` | Officer Resolution Evidence (After Repair) | Cleared municipal culvert & dry carriageway | [Unsplash photo-1584467735871-8e85353a8413](https://images.unsplash.com/photo-1584467735871-8e85353a8413) | Unsplash Open Library | Unsplash Free License (Commercial & Non-Commercial, CC0 compatible) | ~116 KB | `6b8e7c04b7fb3a4ce0c24f867298c6a7cb2a007028bd4049f72af4371f4b8cf6` |

---

## 2. Cryptographic Integrity & Guard Guarantees

1. **Distinct SHA-256 Hashes:** Both images produce distinct 256-bit digests. This prevents backend false-positive deduplication collisions when transitioning through the verification guard.
2. **Size Optimization:** Both images are compressed well under the 500 KB ceiling (64 KB and 116 KB) for responsive mobile viewport rendering.
3. **Verification Boundary Notice:** In the user interface, both photographs are labeled with a visible `Demo evidence` watermark badge to differentiate simulated demonstration assets from field-captured evidence.
