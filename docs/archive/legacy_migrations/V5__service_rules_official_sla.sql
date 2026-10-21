-- ==============================================================================
-- Nirvaha Civic Platform — Database Migration: Official SLA Verification Standards
-- Migration: V5__service_rules_official_sla.sql
-- Purpose: Add official SLA verification status and statutory reference columns to service_rules.
-- Safety: Fully idempotent, backward-compatible with existing records.
-- ==============================================================================

ALTER TABLE service_rules 
    ADD COLUMN IF NOT EXISTS is_official_sla BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(50) NOT NULL DEFAULT 'UNVERIFIED',
    ADD COLUMN IF NOT EXISTS statutory_reference VARCHAR(255);
