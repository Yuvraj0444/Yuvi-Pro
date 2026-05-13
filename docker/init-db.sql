-- SkyWays Airlines — PostgreSQL initialization
-- Creates one database per microservice with a dedicated owner role.
-- Run automatically by the postgres container via docker-entrypoint-initdb.d/

-- ── User Service ─────────────────────────────────────────────────────────────
CREATE USER skyways_user PASSWORD 'skyways_pass';
CREATE DATABASE user_db OWNER skyways_user;
GRANT ALL PRIVILEGES ON DATABASE user_db TO skyways_user;

-- ── Flight Service ───────────────────────────────────────────────────────────
CREATE USER skyways_flight PASSWORD 'skyways_pass';
CREATE DATABASE flight_db OWNER skyways_flight;
GRANT ALL PRIVILEGES ON DATABASE flight_db TO skyways_flight;

-- ── Booking Service ──────────────────────────────────────────────────────────
CREATE USER skyways_booking PASSWORD 'skyways_pass';
CREATE DATABASE booking_db OWNER skyways_booking;
GRANT ALL PRIVILEGES ON DATABASE booking_db TO skyways_booking;

-- ── Payment Service ──────────────────────────────────────────────────────────
CREATE USER skyways_payment PASSWORD 'skyways_pass';
CREATE DATABASE payment_db OWNER skyways_payment;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO skyways_payment;

-- ── Saga Orchestrator ────────────────────────────────────────────────────────
CREATE USER skyways_saga PASSWORD 'skyways_pass';
CREATE DATABASE saga_db OWNER skyways_saga;
GRANT ALL PRIVILEGES ON DATABASE saga_db TO skyways_saga;

-- Grant schema-level privileges after connecting to each database
\c user_db
GRANT ALL PRIVILEGES ON SCHEMA public TO skyways_user;

\c flight_db
GRANT ALL PRIVILEGES ON SCHEMA public TO skyways_flight;

\c booking_db
GRANT ALL PRIVILEGES ON SCHEMA public TO skyways_booking;

\c payment_db
GRANT ALL PRIVILEGES ON SCHEMA public TO skyways_payment;

\c saga_db
GRANT ALL PRIVILEGES ON SCHEMA public TO skyways_saga;