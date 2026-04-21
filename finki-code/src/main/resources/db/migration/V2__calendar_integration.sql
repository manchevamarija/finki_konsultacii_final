-- Calendar sync fields on Consultation
ALTER TABLE consultation
    ADD COLUMN IF NOT EXISTS outlook_event_id VARCHAR(512),
    ADD COLUMN IF NOT EXISTS google_calendar_event_id VARCHAR(512),
    ADD COLUMN IF NOT EXISTS calendar_sync_status VARCHAR(20) DEFAULT 'NOT_SYNCED';

-- Table to store per-professor OAuth tokens for calendar providers
CREATE TABLE IF NOT EXISTS professor_calendar_token (
    professor_id         VARCHAR(255) PRIMARY KEY,
    outlook_access_token  TEXT,
    outlook_refresh_token TEXT,
    outlook_token_expiry  TIMESTAMP,
    google_access_token   TEXT,
    google_refresh_token  TEXT,
    google_token_expiry   TIMESTAMP
);
