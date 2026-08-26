CREATE TABLE order_status_history
(
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT      NOT NULL,
    previous_status VARCHAR(50),
    new_status      VARCHAR(50) NOT NULL,
    source          VARCHAR(50) NOT NULL,
    reference_id    VARCHAR(255),
    changed_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_order_status_history_order_changed_at
    ON order_status_history (order_id, changed_at DESC);



CREATE INDEX idx_order_status_history_source
    ON order_status_history (source);

CREATE INDEX idx_order_status_history_reference_id
    ON order_status_history (reference_id);