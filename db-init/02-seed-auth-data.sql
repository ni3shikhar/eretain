-- ==========================================
-- eRetain - Auth Service Seed Data
-- Default admin user and role rate cards
-- ==========================================

\c eretain_auth;

-- Create tables (matching JPA entity definitions)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    employee_id VARCHAR(255) UNIQUE,
    designation VARCHAR(255),
    phone VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_access (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    module_name VARCHAR(255) NOT NULL,
    can_read BOOLEAN DEFAULT FALSE,
    can_write BOOLEAN DEFAULT FALSE,
    can_delete BOOLEAN DEFAULT FALSE,
    is_enabled BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS role_rate_cards (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(255) NOT NULL UNIQUE,
    audax_rate NUMERIC(10,2) NOT NULL,
    fixed_fee_rate NUMERIC(10,2) NOT NULL,
    tm_rate NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Insert default administrator user
-- Password: Admin@123 (BCrypt encoded)
INSERT INTO users (id, username, password, email, first_name, last_name, employee_id, designation, phone, status, is_active, created_at, updated_at)
VALUES (
    1,
    'admin',
    '$2b$10$UpAIPF3h29DollUrBah1hOnuc7kqvsy.9LVmD47t0alXcmR5OgVt6',
    'admin@eretain.com',
    'System',
    'Administrator',
    'EMP001',
    'System Administrator',
    '+1234567890',
    'ACTIVE',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Insert user roles
INSERT INTO user_roles (user_id, role)
VALUES (1, 'ADMINISTRATOR')
ON CONFLICT DO NOTHING;

-- Insert default PMO user
-- Password: Pmo@123 (BCrypt encoded)
INSERT INTO users (id, username, password, email, first_name, last_name, employee_id, designation, phone, status, is_active, created_at, updated_at)
VALUES (
    2,
    'pmo_user',
    '$2b$10$U0.4/WactHoq6yI15P/MPue6fH9ctjjaWzXntUkyEXxXNpUYrYWHG',
    'pmo@eretain.com',
    'PMO',
    'Manager',
    'EMP002',
    'Project Management Officer',
    '+1234567891',
    'ACTIVE',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
VALUES (2, 'PMO')
ON CONFLICT DO NOTHING;

-- Insert default Employee user
-- Password: Emp@123 (BCrypt encoded)
INSERT INTO users (id, username, password, email, first_name, last_name, employee_id, designation, phone, status, is_active, created_at, updated_at)
VALUES (
    3,
    'employee_user',
    '$2b$10$EniQ/5NJGuzTEvpW4Ug2M.RktFJBtyoIj3uvd7VbNBMMZDmjABjGm',
    'employee@eretain.com',
    'John',
    'Employee',
    'EMP003',
    'Software Engineer',
    '+1234567892',
    'ACTIVE',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
VALUES (3, 'EMPLOYEE')
ON CONFLICT DO NOTHING;

-- Insert Role Rate Cards (3 rate types: Audax, Fixed Fee, T&M)
INSERT INTO role_rate_cards (id, role_name, audax_rate, fixed_fee_rate, tm_rate, currency, description, is_active, created_at, updated_at)
VALUES
    (1, 'Software Engineer', 75.00, 85.00, 95.00, 'USD', 'Standard software engineer rate', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Senior Software Engineer', 100.00, 115.00, 130.00, 'USD', 'Senior software engineer rate', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Tech Lead', 125.00, 140.00, 160.00, 'USD', 'Technical lead rate', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'Project Manager', 110.00, 125.00, 145.00, 'USD', 'Project manager rate', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'Business Analyst', 90.00, 105.00, 120.00, 'USD', 'Business analyst rate', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 'QA Engineer', 70.00, 80.00, 92.00, 'USD', 'Quality assurance engineer rate', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 'DevOps Engineer', 95.00, 110.00, 125.00, 'USD', 'DevOps engineer rate', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (8, 'UX Designer', 85.00, 98.00, 112.00, 'USD', 'UX designer rate', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Insert user access permissions
-- Admin has access to all modules
INSERT INTO user_access (user_id, module_name, can_read, can_write, can_delete, is_enabled, is_active, created_at, updated_at)
VALUES
    (1, 'Dashboard', true, true, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Company Structure', true, true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Projects', true, true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Allocations', true, true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Timesheets', true, true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Rate Cards', true, true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Reports', true, true, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Admin Settings', true, true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- PMO has access to most modules EXCEPT Company Structure
INSERT INTO user_access (user_id, module_name, can_read, can_write, can_delete, is_enabled, is_active, created_at, updated_at)
VALUES
    (2, 'Dashboard', true, true, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Projects', true, true, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Allocations', true, true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Timesheets', true, true, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Rate Cards', true, false, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Reports', true, false, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Employee has limited access
INSERT INTO user_access (user_id, module_name, can_read, can_write, can_delete, is_enabled, is_active, created_at, updated_at)
VALUES
    (3, 'Dashboard', true, false, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Projects', true, false, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Allocations', true, false, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Timesheets', true, true, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Reports', true, false, false, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Reset sequence
SELECT setval('users_id_seq', 3);
SELECT setval('role_rate_cards_id_seq', 8);
