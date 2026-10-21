-- ==============================================================================
-- Nirvaha Civic Platform — Database Migration: Resident Feedback Unique Resolution Index
-- Migration: V4__resident_feedback_resolution_unique_index.sql
-- Purpose: Enforce at most one resident feedback per non-null resolution ID via partial unique index.
-- Safety: Fully idempotent, allows multiple NULL resolution_id entries, no row locks on empty table.
-- ==============================================================================

CREATE UNIQUE INDEX IF NOT EXISTS uq_incident_feedbacks_resolution 
ON incident_feedbacks(resolution_id) 
WHERE resolution_id IS NOT NULL;
