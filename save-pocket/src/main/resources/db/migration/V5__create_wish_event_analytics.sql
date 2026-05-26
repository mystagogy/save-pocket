CREATE TABLE wish_event_analytics_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    expired_count INT NOT NULL DEFAULT 0,
    purchased_count INT NOT NULL DEFAULT 0,
    expired_amount BIGINT NOT NULL DEFAULT 0,
    purchased_amount BIGINT NOT NULL DEFAULT 0,
    net_amount BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT ux_wish_event_analytics_daily_user_date UNIQUE (user_id, stat_date)
);

CREATE TABLE wish_event_analytics_monthly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stat_year INT NOT NULL,
    stat_month INT NOT NULL,
    expired_count INT NOT NULL DEFAULT 0,
    purchased_count INT NOT NULL DEFAULT 0,
    expired_amount BIGINT NOT NULL DEFAULT 0,
    purchased_amount BIGINT NOT NULL DEFAULT 0,
    net_amount BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT ux_wish_event_analytics_monthly_user_year_month UNIQUE (user_id, stat_year, stat_month)
);

CREATE TABLE wish_event_analytics_checkpoint (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    processed_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_wish_event_analytics_daily_user_date
    ON wish_event_analytics_daily (user_id, stat_date);

CREATE INDEX idx_wish_event_analytics_monthly_user_year_month
    ON wish_event_analytics_monthly (user_id, stat_year, stat_month);
