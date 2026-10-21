-- ==============================================================================
-- Nirvaha Civic Platform — Database Migration: Demo Run Ownership
-- Migration: V1__demo_run_ownership.sql
-- Purpose: Add demo_run_id column to core operational tables with indexes.
-- Safety: Fully idempotent, nullable, backward-compatible with existing records.
-- Zero-Downtime: Safe on PostgreSQL (metadata-only column add, no row locks).
-- ==============================================================================

-- 1. Incidents Table
ALTER TABLE incidents 
    ADD COLUMN IF NOT EXISTS demo_run_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_incidents_demo_run_id 
    ON incidents(demo_run_id);

-- 2. Evidence Table
ALTER TABLE evidences 
    ADD COLUMN IF NOT EXISTS demo_run_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_evidences_demo_run_id 
    ON evidences(demo_run_id);

-- 3. Resolutions Table
ALTER TABLE resolutions 
    ADD COLUMN IF NOT EXISTS demo_run_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_resolutions_demo_run_id 
    ON resolutions(demo_run_id);

-- 4. Incident Actions (Audit Log) Table
ALTER TABLE incident_actions 
    ADD COLUMN IF NOT EXISTS demo_run_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_incident_actions_demo_run_id 
    ON incident_actions(demo_run_id);
