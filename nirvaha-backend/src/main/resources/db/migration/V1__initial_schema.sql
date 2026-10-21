-- ==============================================================================
-- Nirvaha Civic Intelligence Platform — Database Initialization Baseline
-- Migration: V1__initial_schema.sql
-- Description: Complete initial schema provisioning for PostgreSQL 16+.
-- Designed for deterministic production deployment with Hibernate ddl-auto=validate.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Core Users Table
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_role_check CHECK (role IN ('CITIZEN', 'OFFICER', 'ADMIN'))
);

-- ------------------------------------------------------------------------------
-- 2. Municipal Wards
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wards (
    id BIGSERIAL PRIMARY KEY,
    ward_number INTEGER NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 3. External & Reference Data Sources
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    provider VARCHAR(150) NOT NULL,
    source_type VARCHAR(255) NOT NULL,
    source_url VARCHAR(500),
    dataset_name VARCHAR(200),
    version VARCHAR(50),
    license VARCHAR(100),
    update_frequency VARCHAR(100),
    coverage VARCHAR(200),
    retrieval_method VARCHAR(100),
    retrieved_at TIMESTAMP,
    published_at TIMESTAMP,
    checksum VARCHAR(128),
    status VARCHAR(50),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT data_sources_source_type_check CHECK (source_type IN (
        'OFFICIAL_GIS', 'OFFICIAL_WEATHER', 'OFFICIAL_POLICY', 'OGD_DATASET',
        'OPEN_GEOGRAPHIC', 'NIRVAHA_CITIZEN', 'SIMULATED'
    ))
);

-- ------------------------------------------------------------------------------
-- 4. OpenStreetMap Road Segments
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS road_segments (
    id BIGSERIAL PRIMARY KEY,
    osm_feature_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    road_reference VARCHAR(50),
    highway_type VARCHAR(50) NOT NULL,
    surface VARCHAR(50),
    is_oneway BOOLEAN,
    max_speed VARCHAR(20),
    length_meters DOUBLE PRECISION,
    min_lat DOUBLE PRECISION NOT NULL,
    max_lat DOUBLE PRECISION NOT NULL,
    min_lng DOUBLE PRECISION NOT NULL,
    max_lng DOUBLE PRECISION NOT NULL,
    geometry_json TEXT NOT NULL,
    geometry_wkt TEXT,
    source VARCHAR(100) NOT NULL,
    license VARCHAR(150) NOT NULL,
    extract_version VARCHAR(50),
    provenance_class VARCHAR(50) NOT NULL,
    validation_status VARCHAR(255) NOT NULL,
    rejection_reason VARCHAR(500),
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT road_segments_validation_status_check CHECK (validation_status IN ('VERIFIED', 'REVIEW_REQUIRED', 'INVALID'))
);

CREATE INDEX IF NOT EXISTS idx_road_bbox ON road_segments (min_lat, max_lat, min_lng, max_lng);

-- ------------------------------------------------------------------------------
-- 5. Administrative Areas & Boundaries
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS administrative_areas (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    official_code VARCHAR(50),
    area_type VARCHAR(255) NOT NULL,
    parent_area_id BIGINT REFERENCES administrative_areas(id) ON DELETE SET NULL,
    min_lat DOUBLE PRECISION,
    max_lat DOUBLE PRECISION,
    min_lng DOUBLE PRECISION,
    max_lng DOUBLE PRECISION,
    geometry_json TEXT,
    geometry_wkt TEXT,
    geometry_status VARCHAR(255) NOT NULL,
    validation_status VARCHAR(255) NOT NULL,
    extraction_method VARCHAR(250),
    reviewer_notes VARCHAR(1000),
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    source_id BIGINT REFERENCES data_sources(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT administrative_areas_area_type_check CHECK (area_type IN ('UT', 'REGION', 'MUNICIPALITY', 'COMMUNE', 'WARD', 'VILLAGE')),
    CONSTRAINT administrative_areas_geometry_status_check CHECK (geometry_status IN ('OFFICIAL', 'DERIVED_FROM_OFFICIAL', 'APPROXIMATE', 'UNVERIFIED')),
    CONSTRAINT administrative_areas_validation_status_check CHECK (validation_status IN ('VERIFIED', 'REVIEW_REQUIRED', 'INVALID'))
);

-- ------------------------------------------------------------------------------
-- 6. Statutory Service Rules & SLA Definitions
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS service_rules (
    id BIGSERIAL PRIMARY KEY,
    service_category VARCHAR(255) NOT NULL,
    service_description VARCHAR(500),
    department VARCHAR(150) NOT NULL,
    jurisdiction VARCHAR(150) NOT NULL,
    target_days INTEGER NOT NULL,
    business_day_rule VARCHAR(50),
    escalation_reference VARCHAR(250),
    notes VARCHAR(1000),
    source_id BIGINT REFERENCES data_sources(id) ON DELETE SET NULL,
    effective_from TIMESTAMP,
    effective_to TIMESTAMP,
    is_official_sla BOOLEAN NOT NULL DEFAULT FALSE,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'UNVERIFIED',
    statutory_reference VARCHAR(255),
    warning_threshold_days INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT service_rules_service_category_check CHECK (service_category IN (
        'POTHOLE', 'DRAINAGE', 'GARBAGE', 'STREETLIGHT', 'WATER_SUPPLY', 'OTHER'
    ))
);

-- ------------------------------------------------------------------------------
-- 7. Weather Observations & Meteorological Telemetry
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS weather_observations (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(100) NOT NULL,
    station_or_district_code VARCHAR(100) NOT NULL,
    observation_type VARCHAR(100) NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    period_start TIMESTAMP,
    period_end TIMESTAMP,
    rainfall_mm DOUBLE PRECISION,
    normal_rainfall_mm DOUBLE PRECISION,
    departure_percent DOUBLE PRECISION,
    rainfall_category VARCHAR(50),
    source_record_id VARCHAR(100),
    raw_payload_hash VARCHAR(128),
    provenance_status VARCHAR(50),
    data_version VARCHAR(50),
    source_id BIGINT REFERENCES data_sources(id) ON DELETE SET NULL,
    retrieved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uktktu3b6kads3hkcira4kbgb9q UNIQUE (provider, station_or_district_code, observation_type, observed_at)
);

-- ------------------------------------------------------------------------------
-- 8. Password Reset Tokens
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user ON password_reset_tokens(user_id);

-- ------------------------------------------------------------------------------
-- 9. Ward Risk Assessments
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS risk_assessments (
    id BIGSERIAL PRIMARY KEY,
    ward_id BIGINT NOT NULL REFERENCES wards(id) ON DELETE CASCADE,
    category VARCHAR(255) NOT NULL,
    risk_level VARCHAR(255) NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    recurrence_score DOUBLE PRECISION NOT NULL,
    unresolved_load_score DOUBLE PRECISION NOT NULL,
    spatial_concentration_score DOUBLE PRECISION NOT NULL,
    recent_activity_score DOUBLE PRECISION NOT NULL,
    historical_score DOUBLE PRECISION NOT NULL,
    weather_impact_score DOUBLE PRECISION NOT NULL,
    weather_context VARCHAR(500),
    explanation VARCHAR(2000) NOT NULL,
    recommended_action VARCHAR(1000),
    assessment_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT risk_assessments_category_check CHECK (category IN (
        'POTHOLE', 'DRAINAGE', 'GARBAGE', 'STREETLIGHT', 'WATER_SUPPLY', 'OTHER'
    )),
    CONSTRAINT risk_assessments_risk_level_check CHECK (risk_level IN ('LOW', 'MODERATE', 'HIGH', 'CRITICAL'))
);

-- ------------------------------------------------------------------------------
-- 10. Incidents (Primary Operational Entity)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    category VARCHAR(255) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    address VARCHAR(500),
    assigned_area_id BIGINT REFERENCES administrative_areas(id) ON DELETE SET NULL,
    assignment_method VARCHAR(255),
    assignment_confidence NUMERIC(5, 4),
    ward_assignment_notes VARCHAR(500),
    assignment_timestamp TIMESTAMP,
    classification_tag VARCHAR(255) NOT NULL DEFAULT 'OFFICIAL',
    data_source_id BIGINT REFERENCES data_sources(id) ON DELETE SET NULL,
    intake_source VARCHAR(50),
    external_source_identifier VARCHAR(100),
    external_record_id VARCHAR(100),
    source_timestamp TIMESTAMP,
    retrieval_timestamp TIMESTAMP,
    ingestion_outcome VARCHAR(50),
    original_language VARCHAR(10),
    original_description VARCHAR(4000),
    normalized_description VARCHAR(4000),
    language_confidence DOUBLE PRECISION,
    language_provider VARCHAR(100),
    normalization_status VARCHAR(50),
    demo_run_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT incidents_category_check CHECK (category IN (
        'POTHOLE', 'DRAINAGE', 'GARBAGE', 'STREETLIGHT', 'WATER_SUPPLY', 'OTHER'
    )),
    CONSTRAINT incidents_severity_check CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT incidents_status_check CHECK (status IN ('REPORTED', 'VERIFIED', 'IN_PROGRESS', 'RESOLVED')),
    CONSTRAINT incidents_assignment_method_check CHECK (assignment_method IS NULL OR assignment_method IN ('POINT_IN_POLYGON', 'MANUAL_ASSIGNMENT', 'GEOCODER', 'APPROXIMATE', 'UNASSIGNED')),
    CONSTRAINT incidents_classification_tag_check CHECK (classification_tag IN (
        'OFFICIAL', 'CITIZEN', 'DERIVED', 'SIMULATED'
    )),
    CONSTRAINT incidents_intake_source_check CHECK (intake_source IS NULL OR intake_source IN (
        'RESIDENT_SUBMITTED', 'AUTHORIZED_GOVERNMENT_SOURCE', 'OFFICIAL_EXTERNAL_DATA', 'SIMULATED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_incidents_demo_run_id ON incidents(demo_run_id);
CREATE INDEX IF NOT EXISTS idx_incidents_reporter ON incidents(reporter_id);
CREATE INDEX IF NOT EXISTS idx_incidents_status ON incidents(status);
CREATE INDEX IF NOT EXISTS idx_incidents_category ON incidents(category);

-- ------------------------------------------------------------------------------
-- 11. Intelligence Signals
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS intelligence_signals (
    id BIGSERIAL PRIMARY KEY,
    signal_type VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    administrative_area_id BIGINT REFERENCES administrative_areas(id) ON DELETE SET NULL,
    area_name VARCHAR(150),
    time_window VARCHAR(255) NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    incident_count INTEGER NOT NULL,
    unresolved_count INTEGER NOT NULL,
    overdue_count INTEGER,
    previous_period_count INTEGER,
    change_percentage DOUBLE PRECISION,
    trend_direction VARCHAR(255),
    centroid_lat DOUBLE PRECISION,
    centroid_lng DOUBLE PRECISION,
    radius_meters DOUBLE PRECISION,
    risk_summary DOUBLE PRECISION,
    priority_summary DOUBLE PRECISION,
    explanation_what VARCHAR(500),
    explanation_where VARCHAR(500),
    explanation_when VARCHAR(500),
    explanation_why VARCHAR(1000),
    explanation_evidence VARCHAR(1000),
    explanation_source VARCHAR(500),
    model_version VARCHAR(50) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT intelligence_signals_signal_type_check CHECK (signal_type IN (
        'HOTSPOT', 'TREND', 'EMERGING_ISSUE', 'UNRESOLVED_LOAD', 'RECURRING_PROBLEM',
        'CROSS_SIGNAL', 'RESOLUTION_PERFORMANCE'
    )),
    CONSTRAINT intelligence_signals_category_check CHECK (category IN (
        'POTHOLE', 'DRAINAGE', 'GARBAGE', 'STREETLIGHT', 'WATER_SUPPLY', 'OTHER'
    )),
    CONSTRAINT intelligence_signals_time_window_check CHECK (time_window IN ('H24', 'D7', 'D30', 'D90')),
    CONSTRAINT intelligence_signals_trend_direction_check CHECK (trend_direction IS NULL OR trend_direction IN ('INCREASING', 'STABLE', 'DECREASING', 'INSUFFICIENT_DATA'))
);

-- ------------------------------------------------------------------------------
-- 12. Incident Evidence & Files
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evidence (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    evidence_type VARCHAR(255) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    file_size BIGINT,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    captured_at TIMESTAMP,
    description VARCHAR(1000),
    content_hash VARCHAR(64),
    demo_run_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT evidence_evidence_type_check CHECK (evidence_type IN ('PHOTO', 'VIDEO', 'DOCUMENT'))
);

CREATE INDEX IF NOT EXISTS idx_evidence_content_hash ON evidence(content_hash);
CREATE INDEX IF NOT EXISTS idx_evidence_incident_id ON evidence(incident_id);
CREATE INDEX IF NOT EXISTS idx_evidence_demo_run_id ON evidence(demo_run_id);

-- ------------------------------------------------------------------------------
-- 13. Incident Actions & Audit Trail
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident_actions (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    officer_id BIGINT NOT NULL REFERENCES users(id),
    action_type VARCHAR(255) NOT NULL,
    remarks VARCHAR(2000),
    demo_run_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT incident_actions_action_type_check CHECK (action_type IN (
        'ASSIGNED', 'VERIFIED', 'STARTED', 'PROGRESS_UPDATE', 'RESOLVED',
        'REOPENED', 'RESIDENT_CONFIRMED', 'RESIDENT_DISPUTED', 'DISPUTE_REVIEWED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_incident_actions_incident ON incident_actions(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_actions_demo_run_id ON incident_actions(demo_run_id);

-- ------------------------------------------------------------------------------
-- 14. Incident SLA Tracking & Escalation Engine
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident_service_tracking (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    service_rule_id BIGINT NOT NULL REFERENCES service_rules(id),
    clock_started_at TIMESTAMP NOT NULL,
    due_at TIMESTAMP NOT NULL,
    elapsed_days BIGINT NOT NULL,
    sla_status VARCHAR(255) NOT NULL,
    last_evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    escalation_level INTEGER NOT NULL DEFAULT 0,
    approaching_notified_at TIMESTAMP,
    overdue_notified_at TIMESTAMP,
    CONSTRAINT incident_service_tracking_sla_status_check CHECK (sla_status IN (
        'NOT_APPLICABLE', 'WITHIN_STANDARD', 'APPROACHING_DEADLINE', 'OVERDUE', 'PAUSED', 'COMPLETED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_incident_service_tracking_incident ON incident_service_tracking(incident_id);

-- ------------------------------------------------------------------------------
-- 15. Real-Time System & User Notifications
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    deduplication_key VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_notifications_dedup_key ON notifications (deduplication_key) WHERE deduplication_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created ON notifications (recipient_id, created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read ON notifications (recipient_id, is_read);

-- ------------------------------------------------------------------------------
-- 16. Officer Decisions & AI Recommendation Overrides
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS officer_decisions (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    decided_by BIGINT NOT NULL REFERENCES users(id),
    recommended_action VARCHAR(255) NOT NULL,
    officer_decision VARCHAR(255) NOT NULL,
    recommendation_id VARCHAR(255),
    decision_reason VARCHAR(2000),
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT officer_decisions_officer_decision_check CHECK (officer_decision IN ('ACCEPT', 'MODIFY', 'DEFER', 'REJECT')),
    CONSTRAINT officer_decisions_recommended_action_check CHECK (recommended_action IN (
        'FIELD_INSPECTION', 'VERIFY_INCIDENT', 'ASSIGN_DEPARTMENT', 'START_REPAIR',
        'CONTINUE_REPAIR', 'ESCALATE', 'REQUEST_MORE_EVIDENCE', 'MONITOR',
        'REVIEW_ANOMALY', 'VERIFY_RESOLUTION', 'REOPEN_CASE', 'NO_ACTION_REQUIRED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_officer_decisions_incident ON officer_decisions(incident_id);

-- ------------------------------------------------------------------------------
-- 17. Integrity Anomaly Flags
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS anomaly_flags (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    anomaly_type VARCHAR(255) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    related_incident_ids VARCHAR(500),
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT anomaly_flags_anomaly_type_check CHECK (anomaly_type IN (
        'RESOLUTION_EVIDENCE_REUSE', 'BULK_CLOSURE', 'REPEATED_REPORTING'
    )),
    CONSTRAINT anomaly_flags_severity_check CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT anomaly_flags_status_check CHECK (status IN ('OPEN', 'REVIEWED', 'DISMISSED'))
);

CREATE INDEX IF NOT EXISTS idx_anomaly_flags_incident ON anomaly_flags(incident_id);

-- ------------------------------------------------------------------------------
-- 18. Verified Incident Resolutions
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resolutions (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    officer_id BIGINT NOT NULL REFERENCES users(id),
    resolution_evidence_id BIGINT NOT NULL REFERENCES evidence(id),
    resolution_similarity_score DOUBLE PRECISION NOT NULL,
    verification_status VARCHAR(255) NOT NULL,
    remarks VARCHAR(2000),
    demo_run_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT resolutions_verification_status_check CHECK (verification_status IN (
        'PENDING', 'VERIFIED', 'FAILED', 'MANUAL_REVIEW'
    ))
);

CREATE INDEX IF NOT EXISTS idx_resolutions_incident ON resolutions(incident_id);
CREATE INDEX IF NOT EXISTS idx_resolutions_demo_run_id ON resolutions(demo_run_id);

-- ------------------------------------------------------------------------------
-- 19. Resident Feedback & Resolution Disputes
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incident_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    resident_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resolution_id BIGINT REFERENCES resolutions(id) ON DELETE SET NULL,
    feedback_type VARCHAR(255) NOT NULL,
    explanation VARCHAR(2000),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by_officer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    review_notes VARCHAR(2000),
    demo_run_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT incident_feedbacks_feedback_type_check CHECK (feedback_type IN ('CONFIRMED', 'DISPUTED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_incident_feedbacks_resolution ON incident_feedbacks(resolution_id) WHERE resolution_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_incident ON incident_feedbacks(incident_id);
CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_resident ON incident_feedbacks(resident_id);
CREATE INDEX IF NOT EXISTS idx_incident_feedbacks_demo_run_id ON incident_feedbacks(demo_run_id);
