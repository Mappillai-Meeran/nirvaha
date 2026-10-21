-- ==============================================================================
-- Nirvaha Civic Platform — Database Migration: Resident Feedback & Dispute Workflow
-- Migration: V3__resident_feedback_and_disputes.sql
-- Purpose: Create incident_feedbacks table for resident confirmation and dispute tracking.
-- Safety: Fully idempotent, nullable, backward-compatible with existing records.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS incident_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    resident_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resolution_id BIGINT REFERENCES resolutions(id) ON DELETE SET NULL,
    feedback_type VARCHAR(20) NOT NULL,
    explanation VARCHAR(2000),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by_officer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    review_notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    demo_run_id VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_incident ON incident_feedbacks(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_resident ON incident_feedbacks(resident_id);
CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_type ON incident_feedbacks(feedback_type);
CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_demo_run_id ON incident_feedbacks(demo_run_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_incident_feedbacks_resolution ON incident_feedbacks(resolution_id) WHERE resolution_id IS NOT NULL;

-- Update check constraint on incident_actions to accommodate resident feedback actions
ALTER TABLE incident_actions DROP CONSTRAINT IF EXISTS incident_actions_action_type_check;
ALTER TABLE incident_actions ADD CONSTRAINT incident_actions_action_type_check 
    CHECK (action_type IN ('ASSIGNED', 'VERIFIED', 'STARTED', 'PROGRESS_UPDATE', 'RESOLVED', 'REOPENED', 'RESIDENT_CONFIRMED', 'RESIDENT_DISPUTED', 'DISPUTE_REVIEWED'));

