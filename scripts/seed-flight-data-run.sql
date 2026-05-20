-- ============================================================
-- SkyWays Flight Data Seed Script
-- Run as: psql -U skyways -d flight_db -h localhost -f seed-flight-data.sql
-- ============================================================

-- Clean up duplicates
TRUNCATE TABLE seats CASCADE;
DELETE FROM flights;

-- Create fare_classes table if not exists
TRUNCATE TABLE fare_classes;

CREATE INDEX IF NOT EXISTS idx_fare_flight_class ON fare_classes(flight_id, class_type);

GRANT ALL ON fare_classes TO skyways;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO skyways;

-- â”€â”€ Additional Airports â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
INSERT INTO airports (iata_code, name, city, country, timezone) VALUES
  ('BLR', 'Kempegowda International Airport',   'Bengaluru',      'IN', 'Asia/Kolkata'),
  ('HYD', 'Rajiv Gandhi International Airport',  'Hyderabad',      'IN', 'Asia/Kolkata'),
  ('MAA', 'Chennai International Airport',       'Chennai',        'IN', 'Asia/Kolkata'),
  ('CCU', 'Netaji Subhas Chandra Bose Intl',     'Kolkata',        'IN', 'Asia/Kolkata'),
  ('AMD', 'Sardar Vallabhbhai Patel Intl',       'Ahmedabad',      'IN', 'Asia/Kolkata'),
  ('GOI', 'Goa International Airport',           'Goa',            'IN', 'Asia/Kolkata'),
  ('PNQ', 'Pune Airport',                        'Pune',           'IN', 'Asia/Kolkata'),
  ('COK', 'Cochin International Airport',        'Kochi',          'IN', 'Asia/Kolkata'),
  ('AUH', 'Abu Dhabi International Airport',     'Abu Dhabi',      'AE', 'Asia/Dubai'),
  ('DOH', 'Hamad International Airport',         'Doha',           'QA', 'Asia/Qatar'),
  ('KUL', 'Kuala Lumpur International Airport',  'Kuala Lumpur',   'MY', 'Asia/Kuala_Lumpur'),
  ('BKK', 'Suvarnabhumi Airport',                'Bangkok',        'TH', 'Asia/Bangkok'),
  ('ICN', 'Incheon International Airport',       'Seoul',          'KR', 'Asia/Seoul'),
  ('PEK', 'Beijing Capital International',       'Beijing',        'CN', 'Asia/Shanghai'),
  ('FRA', 'Frankfurt Airport',                   'Frankfurt',      'DE', 'Europe/Berlin'),
  ('AMS', 'Amsterdam Schiphol Airport',          'Amsterdam',      'NL', 'Europe/Amsterdam'),
  ('MXP', 'Milan Malpensa Airport',              'Milan',          'IT', 'Europe/Rome'),
  ('MAD', 'Adolfo SuÃ¡rez Madrid-Barajas',        'Madrid',         'ES', 'Europe/Madrid'),
  ('MIA', 'Miami International Airport',         'Miami',          'US', 'America/New_York'),
  ('SFO', 'San Francisco International Airport', 'San Francisco',  'US', 'America/Los_Angeles'),
  ('YYZ', 'Toronto Pearson International',       'Toronto',        'CA', 'America/Toronto'),
  ('GRU', 'SÃ£o Paulo-Guarulhos International',   'SÃ£o Paulo',      'BR', 'America/Sao_Paulo'),
  ('JNB', 'O.R. Tambo International Airport',    'Johannesburg',   'ZA', 'Africa/Johannesburg'),
  ('NBO', 'Jomo Kenyatta International',         'Nairobi',        'KE', 'Africa/Nairobi'),
  ('MEL', 'Melbourne Airport',                   'Melbourne',      'AU', 'Australia/Melbourne'),
  ('AKL', 'Auckland Airport',                    'Auckland',       'NZ', 'Pacific/Auckland')
ON CONFLICT (iata_code) DO NOTHING;

-- â”€â”€ Additional Airlines â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
INSERT INTO airlines (airline_id, iata_code, name, country) VALUES
  ('b0000000-0000-0000-0000-000000000001', 'AI', 'Air India',           'IN'),
  ('b0000000-0000-0000-0000-000000000002', '6E', 'IndiGo',              'IN'),
  ('b0000000-0000-0000-0000-000000000003', 'UK', 'Vistara',             'IN'),
  ('b0000000-0000-0000-0000-000000000004', 'EK', 'Emirates',            'AE'),
  ('b0000000-0000-0000-0000-000000000005', 'EY', 'Etihad Airways',      'AE'),
  ('b0000000-0000-0000-0000-000000000006', 'QR', 'Qatar Airways',       'QA'),
  ('b0000000-0000-0000-0000-000000000007', 'SQ', 'Singapore Airlines',  'SG'),
  ('b0000000-0000-0000-0000-000000000008', 'BA', 'British Airways',     'GB'),
  ('b0000000-0000-0000-0000-000000000009', 'LH', 'Lufthansa',           'DE'),
  ('b0000000-0000-0000-0000-000000000010', 'AF', 'Air France',          'FR'),
  ('b0000000-0000-0000-0000-000000000011', 'AA', 'American Airlines',   'US'),
  ('b0000000-0000-0000-0000-000000000012', 'UA', 'United Airlines',     'US')
ON CONFLICT (iata_code) DO NOTHING;

-- â”€â”€ Flight Generation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
-- Creates flights for today through next 60 days
-- Each route gets 2-3 daily flights with different departure times

DO $$
DECLARE
    v_day         DATE;
    v_dep_time    TIMESTAMPTZ;
    v_arr_time    TIMESTAMPTZ;
    v_flight_id   UUID;
    v_today       DATE := CURRENT_DATE;
BEGIN

FOR v_day IN SELECT generate_series(v_today, v_today + INTERVAL '60 days', '1 day')::date
LOOP

    -- â”€â”€ Domestic India â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    -- DEL â†’ BOM  (2h 20m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '06:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '13:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '20:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '2 hours 20 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000002',
                '6E-' || TO_CHAR(v_day,'MMDD') || '-' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'DEL','BOM', v_dep_time, v_arr_time, 'SCHEDULED', 180, 180);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  3500 + (RANDOM()*1000)::INT, 'INR', 150),
            (v_flight_id,'BUSINESS', 12000 + (RANDOM()*3000)::INT,'INR', 20),
            (v_flight_id,'FIRST',    25000 + (RANDOM()*5000)::INT,'INR', 10);
    END LOOP;

    -- BOM â†’ DEL  (2h 25m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '07:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '14:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '21:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '2 hours 25 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000002',
                '6E-' || TO_CHAR(v_day,'MMDD') || '-B' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'BOM','DEL', v_dep_time, v_arr_time, 'SCHEDULED', 180, 180);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  3600 + (RANDOM()*1000)::INT, 'INR', 150),
            (v_flight_id,'BUSINESS', 12500 + (RANDOM()*3000)::INT,'INR', 20),
            (v_flight_id,'FIRST',    26000 + (RANDOM()*5000)::INT,'INR', 10);
    END LOOP;

    -- DEL â†’ BLR  (2h 45m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '07:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '16:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '2 hours 45 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000003',
                'UK-' || TO_CHAR(v_day,'MMDD') || '-' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'DEL','BLR', v_dep_time, v_arr_time, 'SCHEDULED', 168, 168);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  4500 + (RANDOM()*1500)::INT, 'INR', 140),
            (v_flight_id,'BUSINESS', 15000 + (RANDOM()*4000)::INT,'INR', 18),
            (v_flight_id,'FIRST',    30000 + (RANDOM()*8000)::INT,'INR', 10);
    END LOOP;

    -- BLR â†’ DEL  (2h 50m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '09:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '18:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '2 hours 50 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000003',
                'UK-' || TO_CHAR(v_day,'MMDD') || '-B' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'BLR','DEL', v_dep_time, v_arr_time, 'SCHEDULED', 168, 168);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  4600 + (RANDOM()*1500)::INT, 'INR', 140),
            (v_flight_id,'BUSINESS', 15500 + (RANDOM()*4000)::INT,'INR', 18),
            (v_flight_id,'FIRST',    31000 + (RANDOM()*8000)::INT,'INR', 10);
    END LOOP;

    -- BOM â†’ BLR  (1h 50m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '08:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '17:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '1 hour 50 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000002',
                '6E-' || TO_CHAR(v_day,'MMDD') || '-MB' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'BOM','BLR', v_dep_time, v_arr_time, 'SCHEDULED', 180, 180);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  2800 + (RANDOM()*800)::INT, 'INR', 155),
            (v_flight_id,'BUSINESS', 9500 + (RANDOM()*2000)::INT,'INR', 18),
            (v_flight_id,'FIRST',    20000 + (RANDOM()*4000)::INT,'INR', 7);
    END LOOP;

    -- DEL â†’ HYD  (2h 10m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '10:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '19:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '2 hours 10 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000001',
                'AI-' || TO_CHAR(v_day,'MMDD') || '-H' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'DEL','HYD', v_dep_time, v_arr_time, 'SCHEDULED', 200, 200);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  3200 + (RANDOM()*1000)::INT, 'INR', 165),
            (v_flight_id,'BUSINESS', 11000 + (RANDOM()*3000)::INT,'INR', 24),
            (v_flight_id,'FIRST',    22000 + (RANDOM()*5000)::INT,'INR', 11);
    END LOOP;

    -- â”€â”€ India to International â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    -- DEL â†’ DXB  (3h 30m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '03:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '14:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '3 hours 30 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000004',
                'EK-' || TO_CHAR(v_day,'MMDD') || '-D' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'DEL','DXB', v_dep_time, v_arr_time, 'SCHEDULED', 300, 300);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  18000 + (RANDOM()*5000)::INT, 'INR', 250),
            (v_flight_id,'BUSINESS', 55000 + (RANDOM()*15000)::INT,'INR', 35),
            (v_flight_id,'FIRST',    120000 + (RANDOM()*30000)::INT,'INR', 15);
    END LOOP;

    -- BOM â†’ DXB  (3h 10m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '04:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '23:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '3 hours 10 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000004',
                'EK-' || TO_CHAR(v_day,'MMDD') || '-BD' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'BOM','DXB', v_dep_time, v_arr_time, 'SCHEDULED', 300, 300);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  16000 + (RANDOM()*5000)::INT, 'INR', 250),
            (v_flight_id,'BUSINESS', 50000 + (RANDOM()*15000)::INT,'INR', 35),
            (v_flight_id,'FIRST',    110000 + (RANDOM()*30000)::INT,'INR', 15);
    END LOOP;

    -- DEL â†’ LHR  (8h 30m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '02:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '21:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '8 hours 30 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000008',
                'BA-' || TO_CHAR(v_day,'MMDD') || '-DL' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'DEL','LHR', v_dep_time, v_arr_time, 'SCHEDULED', 280, 280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  45000 + (RANDOM()*15000)::INT, 'INR', 220),
            (v_flight_id,'BUSINESS', 150000 + (RANDOM()*50000)::INT,'INR', 40),
            (v_flight_id,'FIRST',    350000 + (RANDOM()*80000)::INT,'INR', 20);
    END LOOP;

    -- BOM â†’ LHR  (9h 30m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '01:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '9 hours 30 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000001',
                'AI-' || TO_CHAR(v_day,'MMDD') || '-BL' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'BOM','LHR', v_dep_time, v_arr_time, 'SCHEDULED', 250, 250);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  42000 + (RANDOM()*12000)::INT, 'INR', 200),
            (v_flight_id,'BUSINESS', 140000 + (RANDOM()*40000)::INT,'INR', 36),
            (v_flight_id,'FIRST',    320000 + (RANDOM()*70000)::INT,'INR', 14);
    END LOOP;

    -- DEL â†’ SIN  (5h 45m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '06:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'),
        ((v_day + TIME '22:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '5 hours 45 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000007',
                'SQ-' || TO_CHAR(v_day,'MMDD') || '-DS' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'DEL','SIN', v_dep_time, v_arr_time, 'SCHEDULED', 280, 280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  28000 + (RANDOM()*8000)::INT, 'INR', 230),
            (v_flight_id,'BUSINESS', 90000 + (RANDOM()*20000)::INT,'INR', 36),
            (v_flight_id,'FIRST',    200000 + (RANDOM()*50000)::INT,'INR', 14);
    END LOOP;

    -- â”€â”€ International Routes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    -- JFK â†’ LHR  (7h)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '22:00')::TIMESTAMPTZ AT TIME ZONE 'America/New_York'),
        ((v_day + TIME '10:00')::TIMESTAMPTZ AT TIME ZONE 'America/New_York')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '7 hours';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000008',
                'BA-' || TO_CHAR(v_day,'MMDD') || '-JL' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'JFK','LHR', v_dep_time, v_arr_time, 'SCHEDULED', 280, 280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  550 + (RANDOM()*200)::INT,  'USD', 220),
            (v_flight_id,'BUSINESS', 2500 + (RANDOM()*500)::INT, 'USD', 40),
            (v_flight_id,'FIRST',    6000 + (RANDOM()*1000)::INT,'USD', 20);
    END LOOP;

    -- LHR â†’ JFK  (8h)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '11:00')::TIMESTAMPTZ AT TIME ZONE 'Europe/London'),
        ((v_day + TIME '15:00')::TIMESTAMPTZ AT TIME ZONE 'Europe/London')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '8 hours';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000008',
                'BA-' || TO_CHAR(v_day,'MMDD') || '-LJ' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'LHR','JFK', v_dep_time, v_arr_time, 'SCHEDULED', 280, 280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  520 + (RANDOM()*200)::INT,  'USD', 220),
            (v_flight_id,'BUSINESS', 2400 + (RANDOM()*500)::INT, 'USD', 40),
            (v_flight_id,'FIRST',    5800 + (RANDOM()*1000)::INT,'USD', 20);
    END LOOP;

    -- JFK â†’ DXB  (12h)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '23:55')::TIMESTAMPTZ AT TIME ZONE 'America/New_York')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '12 hours';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000004',
                'EK-' || TO_CHAR(v_day,'MMDD') || '-JD' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'JFK','DXB', v_dep_time, v_arr_time, 'SCHEDULED', 360, 360);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  850 + (RANDOM()*200)::INT,  'USD', 290),
            (v_flight_id,'BUSINESS', 4500 + (RANDOM()*1000)::INT,'USD', 50),
            (v_flight_id,'FIRST',    12000 + (RANDOM()*2000)::INT,'USD', 20);
    END LOOP;

    -- DXB â†’ DEL  (3h 15m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '09:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Dubai'),
        ((v_day + TIME '19:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Dubai')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '3 hours 15 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000004',
                'EK-' || TO_CHAR(v_day,'MMDD') || '-DD' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'DXB','DEL', v_dep_time, v_arr_time, 'SCHEDULED', 300, 300);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  17000 + (RANDOM()*5000)::INT, 'INR', 250),
            (v_flight_id,'BUSINESS', 52000 + (RANDOM()*15000)::INT,'INR', 35),
            (v_flight_id,'FIRST',    115000 + (RANDOM()*30000)::INT,'INR', 15);
    END LOOP;

    -- SIN â†’ SYD  (7h 45m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '08:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Singapore'),
        ((v_day + TIME '21:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Singapore')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '7 hours 45 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000007',
                'SQ-' || TO_CHAR(v_day,'MMDD') || '-SS' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'SIN','SYD', v_dep_time, v_arr_time, 'SCHEDULED', 280, 280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  420 + (RANDOM()*150)::INT,  'USD', 220),
            (v_flight_id,'BUSINESS', 2200 + (RANDOM()*400)::INT, 'USD', 40),
            (v_flight_id,'FIRST',    5500 + (RANDOM()*800)::INT, 'USD', 20);
    END LOOP;

    -- LHR â†’ SIN  (13h)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '21:00')::TIMESTAMPTZ AT TIME ZONE 'Europe/London')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '13 hours';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000007',
                'SQ-' || TO_CHAR(v_day,'MMDD') || '-LS' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'LHR','SIN', v_dep_time, v_arr_time, 'SCHEDULED', 280, 280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  680 + (RANDOM()*200)::INT,  'USD', 220),
            (v_flight_id,'BUSINESS', 3500 + (RANDOM()*700)::INT, 'USD', 40),
            (v_flight_id,'FIRST',    8500 + (RANDOM()*1500)::INT,'USD', 20);
    END LOOP;

    -- CDG â†’ JFK  (8h 30m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '10:30')::TIMESTAMPTZ AT TIME ZONE 'Europe/Paris')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '8 hours 30 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000010',
                'AF-' || TO_CHAR(v_day,'MMDD') || '-CJ' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'CDG','JFK', v_dep_time, v_arr_time, 'SCHEDULED', 280, 280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  490 + (RANDOM()*180)::INT,  'USD', 220),
            (v_flight_id,'BUSINESS', 2300 + (RANDOM()*500)::INT, 'USD', 40),
            (v_flight_id,'FIRST',    5500 + (RANDOM()*1000)::INT,'USD', 20);
    END LOOP;

    -- NRT â†’ SIN  (7h)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '10:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Tokyo')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '7 hours';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'b0000000-0000-0000-0000-000000000007',
                'SQ-' || TO_CHAR(v_day,'MMDD') || '-NS' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'NRT','SIN', v_dep_time, v_arr_time, 'SCHEDULED', 250, 250);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  380 + (RANDOM()*120)::INT,  'USD', 200),
            (v_flight_id,'BUSINESS', 1800 + (RANDOM()*400)::INT, 'USD', 36),
            (v_flight_id,'FIRST',    4500 + (RANDOM()*800)::INT, 'USD', 14);
    END LOOP;

    -- SkyWays: DEL â†’ JFK  (14h 30m)
    FOR v_dep_time IN VALUES
        ((v_day + TIME '01:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata')
    LOOP
        v_flight_id := gen_random_uuid();
        v_arr_time  := v_dep_time + INTERVAL '14 hours 30 minutes';
        INSERT INTO flights(flight_id, airline_id, flight_number, origin_iata, destination_iata,
                            departure_time, arrival_time, status, total_seats, available_seats)
        VALUES (v_flight_id, 'a0000000-0000-0000-0000-000000000001',
                'SW-' || TO_CHAR(v_day,'MMDD') || '-DJ' || EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,
                'DEL','JFK', v_dep_time, v_arr_time, 'SCHEDULED', 320, 320);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_flight_id,'ECONOMY',  72000 + (RANDOM()*15000)::INT, 'INR', 250),
            (v_flight_id,'BUSINESS', 220000 + (RANDOM()*50000)::INT,'INR', 48),
            (v_flight_id,'FIRST',    500000 + (RANDOM()*100000)::INT,'INR', 22);
    END LOOP;

END LOOP;

END $$;

-- â”€â”€ Summary â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
SELECT 'flights'     AS table_name, COUNT(*) AS rows FROM flights
UNION ALL
SELECT 'fare_classes',               COUNT(*)         FROM fare_classes
UNION ALL
SELECT 'airports',                   COUNT(*)         FROM airports
UNION ALL
SELECT 'airlines',                   COUNT(*)         FROM airlines;
