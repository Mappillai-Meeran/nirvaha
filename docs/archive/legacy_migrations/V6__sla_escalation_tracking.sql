-- V6: SLA Escalation Tracking
-- Adds escalation-level tracking and notification timestamps to incident_service_tracking.
-- Adds configurable warning threshold to service_rules.
-- Non-destructive: adds columns only, no data deletion.

ALTER TABLE incident_service_tracking
    ADD COLUMN IF NOT EXISTS escalation_level INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS approaching_notified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS overdue_notified_at TIMESTAMP;

ALTER TABLE service_rules
    ADD COLUMN IF NOT EXISTS warning_threshold_days INTEGER NOT NULL DEFAULT 3;

-- Drop obsolete enum check constraint on notifications to allow new SLA escalation event types
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_event_type_check;

-- Update existing official drainage rule (3-day target) to use 1-day warning threshold
UPDATE service_rules
SET warning_threshold_days = 1
WHERE service_category = 'DRAINAGE'
  AND is_official_sla = true
  AND department = 'PWD';
