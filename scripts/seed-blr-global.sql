-- Add BLR↔SIN and BLR↔DXB to give Bangalore full global hub connectivity
DO $$
DECLARE
    v_day  DATE;
    v_fid  UUID;
    v_dep  TIMESTAMPTZ;
    v_arr  TIMESTAMPTZ;
    v_today DATE := CURRENT_DATE;
BEGIN
FOR v_day IN SELECT generate_series(v_today, v_today + INTERVAL '60 days', '1 day')::date LOOP

    -- BLR → SIN  02:00 UTC (= 07:30 IST), arrives 10:00 UTC (8h)
    -- Connects to SIN→NRT at 18:00 UTC = 8h layover ✓
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 02:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-RS1','BLR','SIN',v_dep,v_arr,'SCHEDULED',240,240)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',18000+(RANDOM()*5000)::INT,'INR',190),
        (v_fid,'BUSINESS',65000+(RANDOM()*15000)::INT,'INR',34),
        (v_fid,'FIRST',148000+(RANDOM()*40000)::INT,'INR',16);

    -- BLR → SIN  10:00 UTC (= 15:30 IST), arrives 14:30 UTC (4.5h)
    -- Connects to SIN→DEL at 17:00 UTC = 2.5h layover ✓ (for BLR→DEL via SIN alternate)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 10:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-RS2','BLR','SIN',v_dep,v_arr,'SCHEDULED',240,240)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',18500+(RANDOM()*5000)::INT,'INR',190),
        (v_fid,'BUSINESS',66000+(RANDOM()*15000)::INT,'INR',34),
        (v_fid,'FIRST',150000+(RANDOM()*40000)::INT,'INR',16);

    -- SIN → BLR  03:00 UTC (= 11:00 SGT), arrives 07:30 UTC (4.5h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 03:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-SR1','SIN','BLR',v_dep,v_arr,'SCHEDULED',240,240)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',17500+(RANDOM()*5000)::INT,'INR',190),
        (v_fid,'BUSINESS',64000+(RANDOM()*15000)::INT,'INR',34),
        (v_fid,'FIRST',145000+(RANDOM()*40000)::INT,'INR',16);

    -- SIN → BLR  15:00 UTC (= 23:00 SGT), arrives 19:30 UTC (4.5h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 15:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-SR2','SIN','BLR',v_dep,v_arr,'SCHEDULED',240,240)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',18000+(RANDOM()*5000)::INT,'INR',190),
        (v_fid,'BUSINESS',65000+(RANDOM()*15000)::INT,'INR',34),
        (v_fid,'FIRST',147000+(RANDOM()*40000)::INT,'INR',16);

    -- BLR → DXB  01:00 UTC (= 06:30 IST), arrives 05:00 UTC (4h)
    -- DXB→LHR at 10:00 UTC = 5h layover ✓; DXB→DEL at various times ✓
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 01:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-RD1','BLR','DXB',v_dep,v_arr,'SCHEDULED',300,300)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',300+(RANDOM()*100)::INT,'USD',240),
        (v_fid,'BUSINESS',1500+(RANDOM()*400)::INT,'USD',42),
        (v_fid,'FIRST',4000+(RANDOM()*800)::INT,'USD',18);

    -- BLR → DXB  13:00 UTC (= 18:30 IST), arrives 17:00 UTC (4h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 13:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-RD2','BLR','DXB',v_dep,v_arr,'SCHEDULED',300,300)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',310+(RANDOM()*100)::INT,'USD',240),
        (v_fid,'BUSINESS',1550+(RANDOM()*400)::INT,'USD',42),
        (v_fid,'FIRST',4100+(RANDOM()*800)::INT,'USD',18);

    -- DXB → BLR  06:00 UTC (= 10:00 GST), arrives 10:00 UTC (4h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 06:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-DR1','DXB','BLR',v_dep,v_arr,'SCHEDULED',300,300)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',295+(RANDOM()*100)::INT,'USD',240),
        (v_fid,'BUSINESS',1480+(RANDOM()*400)::INT,'USD',42),
        (v_fid,'FIRST',3900+(RANDOM()*800)::INT,'USD',18);

    -- DXB → BLR  18:00 UTC (= 22:00 GST), arrives 22:00 UTC (4h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 18:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-DR2','DXB','BLR',v_dep,v_arr,'SCHEDULED',300,300)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',305+(RANDOM()*100)::INT,'USD',240),
        (v_fid,'BUSINESS',1520+(RANDOM()*400)::INT,'USD',42),
        (v_fid,'FIRST',4000+(RANDOM()*800)::INT,'USD',18);

    -- HYD → SIN  03:30 UTC (= 09:00 IST), arrives 09:30 UTC (6h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 03:30:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '6 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-HS1','HYD','SIN',v_dep,v_arr,'SCHEDULED',220,220)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',19000+(RANDOM()*5000)::INT,'INR',175),
        (v_fid,'BUSINESS',68000+(RANDOM()*15000)::INT,'INR',30),
        (v_fid,'FIRST',152000+(RANDOM()*40000)::INT,'INR',15);

    -- SIN → HYD  11:00 UTC (= 19:00 SGT), arrives 17:00 UTC (6h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 11:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '6 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-SH1','SIN','HYD',v_dep,v_arr,'SCHEDULED',220,220)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',18500+(RANDOM()*5000)::INT,'INR',175),
        (v_fid,'BUSINESS',66000+(RANDOM()*15000)::INT,'INR',30),
        (v_fid,'FIRST',149000+(RANDOM()*40000)::INT,'INR',15);

END LOOP;
END $$;

SELECT 'flights' AS tbl, COUNT(*) FROM flights
UNION ALL SELECT 'fare_classes', COUNT(*) FROM fare_classes
UNION ALL SELECT 'routes', COUNT(DISTINCT origin_iata||destination_iata) FROM flights;