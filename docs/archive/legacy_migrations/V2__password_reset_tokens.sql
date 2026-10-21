-- ==============================================================================
-- Nirvaha Civic Platform — Database Migration: Password Reset Tokens
-- Migration: V2__password_reset_tokens.sql
-- Purpose: Create password_reset_tokens table with secure hashing, expiration, and single-use tracking.
-- Safety: Fully idempotent, backward-compatible.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_hash 
    ON password_reset_tokens(token_hash);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user 
    ON password_reset_tokens(user_id);
