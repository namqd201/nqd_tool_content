-- V5__plans_and_posts.sql
-- Drop legacy v1 tables if present
DROP TABLE IF EXISTS post_variants CASCADE;
DROP TABLE IF EXISTS posts CASCADE;

-- Create content_plans, plan_targets, posts, post_attempts, media_assets, post_media, ai_usage_logs

CREATE TABLE IF NOT EXISTS content_plans (
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
    name VARCHAR(255) NOT NULL,
    topic TEXT NOT NULL,
    instructions TEXT,
    language VARCHAR(10) DEFAULT 'vi',
    tone VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    posts_per_day INT NOT NULL DEFAULT 1,
    time_mode VARCHAR(30) NOT NULL DEFAULT 'FIXED_TIMES',
    time_slots TEXT, -- JSON array of string times
    min_gap_minutes INT DEFAULT 60,
    jitter_minutes INT DEFAULT 0,
    content_mode VARCHAR(30) DEFAULT 'ADAPT_SAME_IDEA',
    include_image BOOLEAN DEFAULT TRUE,
    image_style TEXT,
    image_failure_policy VARCHAR(30) DEFAULT 'POST_WITHOUT_IMAGE',
    generate_lead_hours INT DEFAULT 12,
    missed_grace_minutes INT DEFAULT 120,
    angles TEXT, -- JSON array of topic angles
    estimated_cost_usd NUMERIC(8, 2) DEFAULT 0.00,
    activated_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS plan_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES content_plans(id) ON DELETE CASCADE,
    social_account_id UUID NOT NULL REFERENCES social_accounts(id) ON DELETE CASCADE,
    posts_per_day INT,
    CONSTRAINT uq_plan_targets_plan_account UNIQUE (plan_id, social_account_id)
);

CREATE TABLE IF NOT EXISTS media_assets (
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
    type VARCHAR(30) NOT NULL DEFAULT 'IMAGE',
    source VARCHAR(30) NOT NULL DEFAULT 'AI_GENERATED',
    storage_key VARCHAR(500),
    public_token VARCHAR(64) NOT NULL UNIQUE,
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    width INT,
    height INT,
    sha256 VARCHAR(64),
    prompt TEXT,
    image_provider VARCHAR(50),
    model VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS posts (
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
    plan_id UUID REFERENCES content_plans(id) ON DELETE SET NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'PLAN',
    group_id UUID,
    slot_key VARCHAR(100) NOT NULL,
    social_account_id UUID NOT NULL REFERENCES social_accounts(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    scheduled_timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    generate_at TIMESTAMP WITHOUT TIME ZONE,
    missed_grace_minutes INT DEFAULT 120,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    content TEXT,
    hashtags TEXT, -- JSON array
    thread_parts TEXT, -- JSON array
    image_prompt TEXT,
    content_edited_by_user BOOLEAN DEFAULT FALSE,
    angle TEXT,
    generation_attempts INT DEFAULT 0,
    generation_error TEXT,
    ai_provider_used VARCHAR(50),
    ai_model_used VARCHAR(50),
    generated_at TIMESTAMP WITHOUT TIME ZONE,
    publish_cycle INT DEFAULT 1,
    attempt_count INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    next_attempt_at TIMESTAMP WITHOUT TIME ZONE,
    locked_by VARCHAR(100),
    locked_at TIMESTAMP WITHOUT TIME ZONE,
    lease_expires_at TIMESTAMP WITHOUT TIME ZONE,
    request_sent_at TIMESTAMP WITHOUT TIME ZONE,
    reconcile_count INT DEFAULT 0,
    error_class VARCHAR(50) DEFAULT 'NONE',
    last_error TEXT,
    needs_review_reason TEXT,
    progress TEXT, -- JSON
    platform_post_id VARCHAR(255),
    platform_post_url VARCHAR(1000),
    published_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_posts_plan_account_slot 
    ON posts (plan_id, social_account_id, slot_key) 
    WHERE plan_id IS NOT NULL AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_posts_ready_next_attempt 
    ON posts (next_attempt_at) 
    WHERE status = 'READY';

CREATE INDEX IF NOT EXISTS idx_posts_planned_generate 
    ON posts (generate_at) 
    WHERE status = 'PLANNED';

CREATE INDEX IF NOT EXISTS idx_posts_user_scheduled 
    ON posts (user_id, scheduled_at);

CREATE TABLE IF NOT EXISTS post_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    media_asset_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    order_index INT DEFAULT 0,
    alt_text TEXT
);

CREATE TABLE IF NOT EXISTS post_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    publish_cycle INT NOT NULL DEFAULT 1,
    attempt_no INT NOT NULL DEFAULT 1,
    worker_id VARCHAR(100),
    started_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    request_sent_at TIMESTAMP WITHOUT TIME ZONE,
    finished_at TIMESTAMP WITHOUT TIME ZONE,
    outcome VARCHAR(50) NOT NULL,
    http_status INT,
    platform_error_code VARCHAR(100),
    error_message TEXT,
    response_snippet TEXT
);

CREATE INDEX IF NOT EXISTS idx_post_attempts_post_attempt 
    ON post_attempts (post_id, attempt_no);

CREATE TABLE IF NOT EXISTS ai_usage_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    feature VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(50),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    image_count INT DEFAULT 0,
    latency_ms BIGINT DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    error TEXT,
    estimated_cost_usd NUMERIC(10, 4) DEFAULT 0.0000,
    post_id UUID,
    plan_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_user_created 
    ON ai_usage_logs (user_id, created_at);
