-- ==========================================
-- eRetain - Database Initialization Script
-- Creates all required databases
-- ==========================================

CREATE DATABASE eretain_auth;
CREATE DATABASE eretain_company;
CREATE DATABASE eretain_project;
CREATE DATABASE eretain_allocation;
CREATE DATABASE eretain_timesheet;
CREATE DATABASE eretain_reporting;

-- Grant all privileges to postgres user
GRANT ALL PRIVILEGES ON DATABASE eretain_auth TO postgres;
GRANT ALL PRIVILEGES ON DATABASE eretain_company TO postgres;
GRANT ALL PRIVILEGES ON DATABASE eretain_project TO postgres;
GRANT ALL PRIVILEGES ON DATABASE eretain_allocation TO postgres;
GRANT ALL PRIVILEGES ON DATABASE eretain_timesheet TO postgres;
GRANT ALL PRIVILEGES ON DATABASE eretain_reporting TO postgres;
