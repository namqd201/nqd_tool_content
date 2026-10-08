-- V2__identities.sql
-- Create user_identities table and audit_logs table, adjust users.email nullability

-- Adjust users table email column to allow null
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- Create user_identities table
CREATE TABLE IF NOT EXISTS user_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_user_identities_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_identities_user_id ON user_identities (user_id);
CREATE INDEX IF NOT EXISTS idx_user_identities_provider_email ON user_identities (provider, email);

-- Create audit_logs table (append-only)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    details TEXT,
    ip VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at);

-- Migrate existing users to have a GOOGLE identity record if google_id is present
INSERT INTO user_identities (user_id, provider, provider_user_id, email, created_at, last_login_at)
SELECT id, 'GOOGLE', COALESCE(google_id, email), email, created_at, updated_at
FROM users
WHERE google_id IS NOT NULL OR email IS NOT NULL
ON CONFLICT (provider, provider_user_id) DO NOTHING;
