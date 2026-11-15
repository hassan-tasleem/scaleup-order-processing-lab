-- V1: core tables for the ScaleUp order processing lab.
-- Deliberately NOT created here (they are experiment variables):
--   index on orders (customer_id, status)       -> P04 adds it as the fix
--   unique orders (idempotency_key)             -> P17 adds it as the fix
--   unique notifications (order_id, type)       -> P18 adds it as the fix

CREATE TABLE customers (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku         VARCHAR(50)   NOT NULL UNIQUE,
    name        VARCHAR(150)  NOT NULL,
    category    VARCHAR(50)   NOT NULL,
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE inventory (
    product_id    BIGINT PRIMARY KEY REFERENCES products (id),
    initial_qty   INTEGER NOT NULL CHECK (initial_qty >= 0),
    available_qty INTEGER NOT NULL CHECK (available_qty >= 0),
    reserved_qty  INTEGER NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
    version       BIGINT  NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers (id),
    status          VARCHAR(20) NOT NULL CHECK (status IN
                    ('PENDING','STOCK_RESERVED','PAID','CONFIRMED','PAYMENT_FAILED','CANCELLED')),
    total_amount    NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    idempotency_key VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at    TIMESTAMPTZ
);

CREATE TABLE order_items (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders (id),
    product_id  BIGINT NOT NULL REFERENCES products (id),
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0)
);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE payments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    BIGINT NOT NULL UNIQUE REFERENCES orders (id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS','DECLINED','TIMEOUT','ERROR')),
    amount      NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    attempts    INTEGER NOT NULL DEFAULT 1 CHECK (attempts >= 1),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notifications (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders (id),
    type        VARCHAR(30) NOT NULL,
    status      VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
    attempts    INTEGER NOT NULL DEFAULT 1 CHECK (attempts >= 1),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ
);