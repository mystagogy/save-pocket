CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT ux_users_email UNIQUE (email)
);

CREATE TABLE product_wish (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_url TEXT NOT NULL,
    product_image_url TEXT,
    memo VARCHAR(500),
    reference_price BIGINT,
    user_deal_price BIGINT,
    deal_url TEXT,
    deal_source_type VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    first_registered_at DATETIME(6) NOT NULL,
    last_viewed_at DATETIME(6) NOT NULL,
    expire_at DATETIME(6) NOT NULL,
    expired_at DATETIME(6),
    reactivated_count INT NOT NULL DEFAULT 0,
    saved_amount BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_product_wish_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wish_id BIGINT NOT NULL,
    price_type VARCHAR(20) NOT NULL,
    previous_price BIGINT NOT NULL,
    changed_price BIGINT NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_price_history_wish FOREIGN KEY (wish_id) REFERENCES product_wish (id)
);

CREATE TABLE wish_event_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wish_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    event_at DATETIME(6) NOT NULL,
    description VARCHAR(500),
    metadata TEXT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_wish_event_history_wish FOREIGN KEY (wish_id) REFERENCES product_wish (id)
);

CREATE INDEX idx_wish_user_status_expire ON product_wish (user_id, status, expire_at);
CREATE INDEX idx_wish_status_expire ON product_wish (status, expire_at);
CREATE INDEX idx_wish_user_updated ON product_wish (user_id, updated_at);
CREATE INDEX idx_price_history_wish_changed ON price_history (wish_id, changed_at);
CREATE INDEX idx_event_history_wish_event ON wish_event_history (wish_id, event_at);
