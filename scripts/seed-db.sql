-- =============================================================================
-- SkyWays Airlines — Master Database Seed Script
-- Prerequisites: PostgreSQL 14+ running; connecting as superuser (postgres).
-- Usage:
--   psql -U postgres -f seed-db.sql
--
-- This script:
--   1. Creates the 5 service databases (idempotent — skips if they exist)
--   2. Creates a shared 'skyways' role used by all services
--   3. Applies all table DDL per database
--   4. Seeds reference data (airports, airlines, admin user)
-- =============================================================================

-- Create databases (run as superuser; safe to re-run)
SELECT 'CREATE DATABASE user_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_db')\gexec
SELECT 'CREATE DATABASE flight_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'flight_db')\gexec
SELECT 'CREATE DATABASE booking_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'booking_db')\gexec
SELECT 'CREATE DATABASE payment_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')\gexec
SELECT 'CREATE DATABASE saga_db'
  WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'saga_db')\gexec

-- Create shared application role
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'skyways') THEN
    CREATE ROLE skyways WITH LOGIN PASSWORD 'skyways_pass';
  END IF;
END$$;

-- Grant ownership
GRANT ALL PRIVILEGES ON DATABASE user_db    TO skyways;
GRANT ALL PRIVILEGES ON DATABASE flight_db  TO skyways;
GRANT ALL PRIVILEGES ON DATABASE booking_db TO skyways;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO skyways;
GRANT ALL PRIVILEGES ON DATABASE saga_db    TO skyways;

-- ===========================================================================
-- 1. USER-DB SCHEMA
-- ===========================================================================
\connect user_db;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    user_id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(50)  NOT NULL DEFAULT 'CUSTOMER',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS passenger_profiles (
    profile_id      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    passport_no     TEXT        NOT NULL,
    passport_expiry TEXT        NOT NULL,
    nationality     VARCHAR(100),
    date_of_birth   TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_passenger_profiles_user ON passenger_profiles(user_id);

CREATE TABLE IF NOT EXISTS audit_log (
    log_id      BIGSERIAL    PRIMARY KEY,
    user_id     UUID,
    action      VARCHAR(100),
    ip_address  VARCHAR(50),
    user_agent  TEXT,
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_log_user       ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_occurred   ON audit_log(occurred_at);

-- Seed admin user (password: Admin@123 BCrypt hashed)
INSERT INTO users (email, password_hash, full_name, role)
VALUES ('admin@skyways.com',
        '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'SkyWays Admin', 'ADMIN')
ON CONFLICT (email) DO NOTHING;


-- ===========================================================================
-- 2. FLIGHT-DB SCHEMA
-- ===========================================================================
\connect flight_db;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS airports (
    iata_code CHAR(3)      PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    city      VARCHAR(100),
    country   VARCHAR(100),
    timezone  VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS airlines (
    airline_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    iata_code  CHAR(2)     UNIQUE,
    name       VARCHAR(255) NOT NULL,
    country    VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS flights (
    flight_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    airline_id       UUID        NOT NULL REFERENCES airlines(airline_id),
    flight_number    VARCHAR(10) NOT NULL,
    origin_iata      CHAR(3)     NOT NULL REFERENCES airports(iata_code),
    destination_iata CHAR(3)     NOT NULL REFERENCES airports(iata_code),
    departure_time   TIMESTAMPTZ NOT NULL,
    arrival_time     TIMESTAMPTZ NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    total_seats      INT         NOT NULL,
    available_seats  INT         NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_flights_origin_dest_dep
    ON flights(origin_iata, destination_iata, departure_time);
CREATE INDEX IF NOT EXISTS idx_flights_number ON flights(flight_number);
CREATE INDEX IF NOT EXISTS idx_flights_status  ON flights(status);

CREATE TABLE IF NOT EXISTS fare_classes (
    fare_id    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_id  UUID          NOT NULL REFERENCES flights(flight_id) ON DELETE CASCADE,
    class_type VARCHAR(20)   NOT NULL,
    base_price NUMERIC(10,2) NOT NULL,
    currency   CHAR(3)       NOT NULL DEFAULT 'USD',
    available  INT           NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_fare_classes_flight ON fare_classes(flight_id);

CREATE TABLE IF NOT EXISTS seats (
    seat_id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    flight_id   UUID        NOT NULL REFERENCES flights(flight_id) ON DELETE CASCADE,
    seat_number VARCHAR(5)  NOT NULL,
    class_type  VARCHAR(20),
    is_reserved BOOLEAN     NOT NULL DEFAULT FALSE,
    reserved_at TIMESTAMPTZ,
    booking_id  UUID
);
CREATE INDEX IF NOT EXISTS idx_seats_flight_reserved ON seats(flight_id, is_reserved);

-- Seed reference airports
INSERT INTO airports (iata_code, name, city, country, timezone) VALUES
    ('JFK', 'John F. Kennedy International Airport', 'New York',    'US', 'America/New_York'),
    ('LHR', 'Heathrow Airport',                      'London',      'GB', 'Europe/London'),
    ('DXB', 'Dubai International Airport',           'Dubai',       'AE', 'Asia/Dubai'),
    ('SIN', 'Changi Airport',                        'Singapore',   'SG', 'Asia/Singapore'),
    ('CDG', 'Charles de Gaulle Airport',             'Paris',       'FR', 'Europe/Paris'),
    ('HKG', 'Hong Kong International Airport',       'Hong Kong',   'HK', 'Asia/Hong_Kong'),
    ('SYD', 'Sydney Kingsford Smith Airport',        'Sydney',      'AU', 'Australia/Sydney'),
    ('LAX', 'Los Angeles International Airport',     'Los Angeles', 'US', 'America/Los_Angeles'),
    ('NRT', 'Narita International Airport',          'Tokyo',       'JP', 'Asia/Tokyo'),
    ('ORD', 'O''Hare International Airport',         'Chicago',     'US', 'America/Chicago'),
    ('DEL', 'Indira Gandhi International Airport',   'New Delhi',   'IN', 'Asia/Kolkata'),
    ('BOM', 'Chhatrapati Shivaji Maharaj Int''l',    'Mumbai',      'IN', 'Asia/Kolkata')
ON CONFLICT (iata_code) DO NOTHING;

-- Seed SkyWays airline
INSERT INTO airlines (airline_id, iata_code, name, country)
VALUES ('a0000000-0000-0000-0000-000000000001', 'SW', 'SkyWays Airlines', 'US')
ON CONFLICT (iata_code) DO NOTHING;

-- Seed sample flights (2026-06)
INSERT INTO flights (airline_id, flight_number, origin_iata, destination_iata,
                     departure_time, arrival_time, total_seats, available_seats)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'SW-101', 'JFK', 'LHR',
     '2026-06-01 08:00:00+00', '2026-06-01 20:00:00+00', 180, 180),
    ('a0000000-0000-0000-0000-000000000001', 'SW-102', 'LHR', 'JFK',
     '2026-06-02 10:00:00+00', '2026-06-02 13:00:00+00', 180, 180),
    ('a0000000-0000-0000-0000-000000000001', 'SW-201', 'JFK', 'DXB',
     '2026-06-05 14:00:00+00', '2026-06-06 01:00:00+00', 250, 250),
    ('a0000000-0000-0000-0000-000000000001', 'SW-301', 'DEL', 'LHR',
     '2026-06-10 02:00:00+00', '2026-06-10 10:30:00+00', 320, 320),
    ('a0000000-0000-0000-0000-000000000001', 'SW-401', 'SIN', 'SYD',
     '2026-06-15 22:00:00+00', '2026-06-16 07:30:00+00', 200, 200)
ON CONFLICT DO NOTHING;


-- ===========================================================================
-- 3. BOOKING-DB SCHEMA
-- ===========================================================================
\connect booking_db;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS bookings (
    booking_id   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID          NOT NULL,
    booking_ref  VARCHAR(10)   UNIQUE NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    currency     CHAR(3)       NOT NULL DEFAULT 'USD',
    status       VARCHAR(30)   NOT NULL DEFAULT 'INITIATED',
    saga_id      UUID,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_bookings_user   ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_ref    ON bookings(booking_ref);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_saga   ON bookings(saga_id);

CREATE TABLE IF NOT EXISTS booking_items (
    item_id     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID          NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    flight_id   UUID          NOT NULL,
    seat_id     UUID,
    fare_class  VARCHAR(20),
    price       NUMERIC(10,2),
    segment_seq INT           NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_booking_items_booking ON booking_items(booking_id);

CREATE TABLE IF NOT EXISTS passengers (
    passenger_id  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id    UUID        NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    first_name    TEXT        NOT NULL,
    last_name     TEXT        NOT NULL,
    passport_no   TEXT        NOT NULL,
    nationality   VARCHAR(100),
    date_of_birth TEXT        NOT NULL,
    email         TEXT,
    phone         TEXT
);
CREATE INDEX IF NOT EXISTS idx_passengers_booking ON passengers(booking_id);

CREATE TABLE IF NOT EXISTS booking_status_history (
    history_id BIGSERIAL    PRIMARY KEY,
    booking_id UUID         NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30)  NOT NULL,
    reason     TEXT,
    changed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_status_history_booking ON booking_status_history(booking_id);
CREATE INDEX IF NOT EXISTS idx_status_history_time    ON booking_status_history(changed_at);


-- ===========================================================================
-- 4. PAYMENT-DB SCHEMA
-- ===========================================================================
\connect payment_db;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS payments (
    payment_id       UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id       UUID          NOT NULL UNIQUE,
    amount           NUMERIC(10,2) NOT NULL,
    currency         CHAR(3)       NOT NULL DEFAULT 'USD',
    status              VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    razorpay_order_id   VARCHAR(255)  UNIQUE,
    gateway_payment_id  VARCHAR(255),
    idempotency_key     VARCHAR(255)  UNIQUE NOT NULL,
    saga_id             VARCHAR(255),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_payments_booking  ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_idempkey ON payments(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_payments_status   ON payments(status);

CREATE TABLE IF NOT EXISTS payment_transactions (
    txn_id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id   UUID        NOT NULL REFERENCES payments(payment_id) ON DELETE CASCADE,
    gateway_event VARCHAR(100),
    raw_response TEXT,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_payment_txn_payment ON payment_transactions(payment_id);

CREATE TABLE IF NOT EXISTS refunds (
    refund_id        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id       UUID          NOT NULL REFERENCES payments(payment_id) ON DELETE CASCADE,
    refund_amount    NUMERIC(10,2) NOT NULL,
    gateway_refund_id VARCHAR(255),
    reason           VARCHAR(255),
    status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_refunds_payment ON refunds(payment_id);


-- ===========================================================================
-- 5. SAGA-DB SCHEMA
-- ===========================================================================
\connect saga_db;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS saga_state (
    saga_id         UUID        PRIMARY KEY,
    booking_id      UUID        NOT NULL UNIQUE,
    current_step      VARCHAR(60) NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    last_event_type   VARCHAR(100),
    failure_reason    TEXT,
    compensation_step VARCHAR(50),
    compensating      BOOLEAN     NOT NULL DEFAULT FALSE,
    retry_count       INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_saga_booking ON saga_state(booking_id);
CREATE INDEX IF NOT EXISTS idx_saga_status  ON saga_state(status);

CREATE TABLE IF NOT EXISTS saga_step_history (
    step_id      BIGSERIAL    PRIMARY KEY,
    saga_id      UUID         NOT NULL REFERENCES saga_state(saga_id),
    step_name    VARCHAR(60)  NOT NULL,
    event_type   VARCHAR(100),
    outcome      VARCHAR(20)  NOT NULL,
    error_detail TEXT,
    executed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_saga_step_saga ON saga_step_history(saga_id);
CREATE INDEX IF NOT EXISTS idx_saga_step_time ON saga_step_history(executed_at);

-- ===========================================================================
-- End of seed script
-- ===========================================================================