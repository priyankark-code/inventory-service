CREATE TABLE inventory_items
(
    product_id         VARCHAR(100) PRIMARY KEY,
    available_quantity INTEGER     NOT NULL,
    reserved_quantity  INTEGER     NOT NULL DEFAULT 0,
    version             BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT chk_inventory_available_nonnegative
        CHECK (available_quantity >= 0),

    CONSTRAINT chk_inventory_reserved_nonnegative
        CHECK (reserved_quantity >= 0)
);

CREATE TABLE inventory_reservations
(
    id         UUID PRIMARY KEY,
    order_id   UUID                     NOT NULL,
    product_id VARCHAR(100)             NOT NULL,
    quantity   INTEGER                  NOT NULL,
    status     VARCHAR(30)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_inventory_reservation_product
        FOREIGN KEY (product_id)
            REFERENCES inventory_items (product_id),

    CONSTRAINT uq_inventory_reservation_order_product
        UNIQUE (order_id, product_id),

    CONSTRAINT chk_inventory_reservation_quantity
        CHECK (quantity > 0)
);

CREATE TABLE processed_events
(
    event_id     UUID PRIMARY KEY,
    event_type   VARCHAR(100)             NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_inventory_reservations_order
    ON inventory_reservations (order_id);