-- V4__social_accounts.sql
-- Create social_connections and social_accounts tables

CREATE TABLE IF NOT EXISTS social_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL,
    platform_user_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    scopes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    access_token_enc TEXT NOT NULL,
    refresh_token_enc TEXT,
    key_version INT NOT NULL DEFAULT 1,
    token_expires_at TIMESTAMP WITHOUT TIME ZONE,
    refresh_token_expires_at TIMESTAMP WITHOUT TIME ZONE,
    last_refreshed_at TIMESTAMP WITHOUT TIME ZONE,
    refresh_failure_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    connected_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_social_connections_user_platform_deleted 
    ON social_connections (user_id, platform) 
    WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS social_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    connection_id UUID NOT NULL REFERENCES social_connections(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL,
    account_type VARCHAR(50) NOT NULL DEFAULT 'PAGE',
    platform_account_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    avatar_url VARCHAR(1000),
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    page_access_token_enc TEXT,
    key_version INT NOT NULL DEFAULT 1,
    last_health_check_at TIMESTAMP WITHOUT TIME ZONE,
    last_error TEXT,
    needs_reauth_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_social_accounts_user_platform_acc_deleted 
    ON social_accounts (user_id, platform, platform_account_id) 
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_social_accounts_connection_id 
    ON social_accounts (connection_id);
