-- Capture TotalGEX
CREATE TABLE totalgex (
    id BIGSERIAL PRIMARY KEY,
    symbol TEXT,
    data JSONB,
    created TIMESTAMP NOT NULL
);

CREATE INDEX idx_totalgex_data ON totalgex USING GIN (data);
CREATE INDEX idx_totalgex_symbol ON totalgex (symbol);