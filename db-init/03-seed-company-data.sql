-- ==========================================
-- eRetain - Company Service Seed Data
-- Sample company structure
-- ==========================================

\c eretain_company;

-- Create tables (matching JPA entity definitions)
CREATE TABLE IF NOT EXISTS business_units (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS units (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    business_unit_id BIGINT NOT NULL REFERENCES business_units(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS delivery_units (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    unit_id BIGINT NOT NULL REFERENCES units(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Insert Business Units
INSERT INTO business_units (id, name, code, description, is_active, created_at, updated_at)
VALUES
    (1, 'Technology Services', 'TS', 'Technology and software development services', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Consulting Services', 'CS', 'Business and management consulting', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Digital Solutions', 'DS', 'Digital transformation and solutions', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Insert Units
INSERT INTO units (id, name, code, description, business_unit_id, is_active, created_at, updated_at)
VALUES
    (1, 'Application Development', 'AD', 'Custom application development', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Infrastructure', 'INFRA', 'Cloud and infrastructure services', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Strategy Consulting', 'SC', 'Business strategy consulting', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'Data & Analytics', 'DA', 'Data engineering and analytics', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Insert Delivery Units
INSERT INTO delivery_units (id, name, code, description, unit_id, is_active, created_at, updated_at)
VALUES
    (1, 'US East Delivery', 'US-E', 'US East coast delivery center', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'US West Delivery', 'US-W', 'US West coast delivery center', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'India Delivery', 'IND', 'India offshore delivery center', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'Cloud Operations', 'CLOUD', 'Cloud infrastructure operations', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'Analytics Lab', 'ALAB', 'Data analytics laboratory', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Reset sequences
SELECT setval('business_units_id_seq', 3);
SELECT setval('units_id_seq', 4);
SELECT setval('delivery_units_id_seq', 5);
