ALTER TABLE product_wish
    ADD COLUMN tracked_product_id VARCHAR(100) NULL;

CREATE INDEX idx_product_wish_tracked_product_id
    ON product_wish (tracked_product_id);
