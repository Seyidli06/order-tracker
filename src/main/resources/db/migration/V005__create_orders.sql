CREATE TABLE orders
(
    id                BIGSERIAL PRIMARY KEY,
    external_order_id VARCHAR(100)  NOT NULL,
    user_id           BIGINT        NOT NULL,
    total_amount      NUMERIC(19,2) NOT NULL,
    currency          VARCHAR(3)    NOT NULL,
    status            VARCHAR(50)   NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_orders_external_order_id
        UNIQUE (external_order_id),

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
);

CREATE INDEX idx_orders_user_created_at
    ON orders (user_id, created_at DESC);

CREATE INDEX idx_orders_status
    ON orders (status);

