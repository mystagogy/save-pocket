CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wish_id BIGINT NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    link_url VARCHAR(300),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notification_wish FOREIGN KEY (wish_id) REFERENCES product_wish (id)
);

CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_user_unread ON notification (user_id, is_read, created_at DESC);
CREATE INDEX idx_notification_dedup ON notification (user_id, wish_id, notification_type, created_at DESC);
