-- Table to store trading symbols (e.g. AAPL, TSLA)
CREATE TABLE symbol (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(10) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Table to store orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    symbol_id BIGINT NOT NULL REFERENCES symbol(id),
    side VARCHAR(4) NOT NULL,          -- BUY or SELL
    price NUMERIC(18, 4) NOT NULL,
    quantity INT NOT NULL,
    filled_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,       -- CREATED, OPEN, PARTIALLY_FILLED, FILLED, CANCELLED, EXPIRED
    tif VARCHAR(10) NOT NULL,          -- GTC, IOC, FOK
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP
);

-- Table to store matched trades
CREATE TABLE trade (
    id BIGSERIAL PRIMARY KEY,
    buy_order_id BIGINT NOT NULL REFERENCES orders(id),
    sell_order_id BIGINT NOT NULL REFERENCES orders(id),
    symbol_id BIGINT NOT NULL REFERENCES symbol(id),
    price NUMERIC(18, 4) NOT NULL,
    quantity INT NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Seed a couple of symbols to test with
INSERT INTO symbol (ticker) VALUES ('AAPL');
INSERT INTO symbol (ticker) VALUES ('TSLA');