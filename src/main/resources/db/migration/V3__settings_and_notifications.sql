-- V3__settings_and_notifications.sql
-- Create user_settings and notifications tables

CREATE TABLE IF NOT EXISTS user_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255),
    updated_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by VARCHAR(255),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    timezone VARCHAR(50) DEFAULT 'Asia/Ho_Chi_Minh',
    default_language VARCHAR(10) DEFAULT 'vi',
    tone VARCHAR(100) DEFAULT 'Chuyên nghiệp, truyền cảm hứng, ngắn gọn',
    writing_style TEXT,
    forbidden_words TEXT,
    preferred_hashtags TEXT,
    default_cta TEXT,
    emoji_policy VARCHAR(30) DEFAULT 'MODERATE',
    extra_guidelines TEXT,
    text_provider_primary VARCHAR(50) DEFAULT 'GEMINI',
    text_provider_fallback VARCHAR(50) DEFAULT 'OPENAI',
    image_provider_primary VARCHAR(50) DEFAULT 'GEMINI_IMAGE',
    image_provider_fallback VARCHAR(50) DEFAULT 'OPENAI_IMAGE',
    ai_daily_budget_usd NUMERIC(8, 2) DEFAULT 5.00,
    append_ai_disclosure BOOLEAN DEFAULT FALSE,
    ai_disclosure_text TEXT,
    notify_telegram_chat_id VARCHAR(100),
    notify_email VARCHAR(255),
    notify_events TEXT,
    daily_digest_enabled BOOLEAN DEFAULT TRUE,
    daily_digest_time TIME WITHOUT TIME ZONE DEFAULT '08:00:00',
    generate_lead_hours INT DEFAULT 12,
    missed_grace_minutes INT DEFAULT 120
);

CREATE TABLE IF NOT EXISTS notifications (
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
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    payload TEXT,
    related_post_id UUID,
    related_plan_id UUID,
    delivery_status VARCHAR(30) DEFAULT 'PENDING',
    channels_sent VARCHAR(255),
    delivery_attempts INT DEFAULT 0,
    next_delivery_at TIMESTAMP WITHOUT TIME ZONE,
    read_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications (user_id, read_at);
CREATE INDEX IF NOT EXISTS idx_notifications_delivery ON notifications (next_delivery_at) WHERE delivery_status = 'PENDING';
