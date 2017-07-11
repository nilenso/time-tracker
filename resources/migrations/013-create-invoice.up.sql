CREATE TABLE invoice (
    id SERIAL PRIMARY KEY,
    client VARCHAR,
    address VARCHAR,
    currency VARCHAR,
    utc_offset INTEGER,
    notes VARCHAR,
    items VARCHAR,
    subtotal FLOAT,
    amount_due FLOAT,
    from_date INTEGER,
    to_date INTEGER,
    tax_amounts VARCHAR,
    paid BOOLEAN
);
