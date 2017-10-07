CREATE TABLE domains
(id SERIAL PRIMARY KEY,
 name VARCHAR(254) UNIQUE,
 timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
 );

CREATE UNIQUE INDEX idx_lower_unique_name  ON domains (lower(name));

