-- Fix NRT→DEL and other cross-regional connections by inserting flights at exact UTC times
-- NRT→SIN arrives 15:00 UTC, so SIN→DEL needs to depart 16:00+ UTC (≥60 min layover)
-- NRT→HKG arrives 11:30 UTC, so HKG→DEL needs to depart 12:30+ UTC
DO $$
DECLARE
    v_day  DATE;
    v_fid  UUID;
    v_dep  TIMESTAMPTZ;
    v_arr  TIMESTAMPTZ;
    v_today DATE := CURRENT_DATE;
BEGIN
FOR v_day IN SELECT generate_series(v_today, v_today + INTERVAL '60 days', '1 day')::date LOOP

    -- SIN → DEL  17:00 UTC  (= 01:00 SGT next day), arrives 22:30 UTC
    -- Gives NRT pax a 2h layover in SIN (NRT arrives 15:00 UTC)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 17:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '5 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-SD3','SIN','DEL',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',27000+(RANDOM()*8000)::INT,'INR',230),
        (v_fid,'BUSINESS',88000+(RANDOM()*20000)::INT,'INR',36),
        (v_fid,'FIRST',195000+(RANDOM()*50000)::INT,'INR',14);

    -- HKG → DEL  13:30 UTC  (= 21:30 HKT), arrives 19:30 UTC
    -- Gives NRT pax a 2h layover in HKG (NRT arrives 11:30 UTC)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 13:30:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '6 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-HD2','HKG','DEL',v_dep,v_arr,'SCHEDULED',250,250)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',33000+(RANDOM()*8000)::INT,'INR',200),
        (v_fid,'BUSINESS',96000+(RANDOM()*20000)::INT,'INR',36),
        (v_fid,'FIRST',212000+(RANDOM()*50000)::INT,'INR',14);

    -- DEL → NRT via HKG: DEL → HKG  05:00 UTC (= 10:30 IST), arrives 11:00 UTC
    -- Then HKG → NRT 06:30 UTC is too early.
    -- Add: HKG → NRT  13:30 UTC (= 21:30 HKT), arrives 18:00 UTC (4.5h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 13:30:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '4 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-HN2','HKG','NRT',v_dep,v_arr,'SCHEDULED',250,250)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',360+(RANDOM()*100)::INT,'USD',200),
        (v_fid,'BUSINESS',1850+(RANDOM()*400)::INT,'USD',36),
        (v_fid,'FIRST',4600+(RANDOM()*800)::INT,'USD',14);

    -- DEL → HKG  10:30 UTC (= 16:00 IST), arrives 16:30 UTC
    -- Lets DEL pax connect to HKG→NRT at 13:30 UTC... that's too early (arrives after)
    -- Add DEL→HKG at 05:30 UTC (= 11:00 IST), arrives 11:30 UTC → HKG→NRT 13:30 UTC = 2h layover
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 05:30:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '6 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-DH2','DEL','HKG',v_dep,v_arr,'SCHEDULED',250,250)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',31000+(RANDOM()*8000)::INT,'INR',200),
        (v_fid,'BUSINESS',94000+(RANDOM()*20000)::INT,'INR',36),
        (v_fid,'FIRST',207000+(RANDOM()*50000)::INT,'INR',14);

    -- NRT → SIN  03:00 UTC (= 12:00 JST), arrives 10:00 UTC
    -- Then SIN→DEL 15:00 UTC = 5h layover ✓ (within 720 min)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 03:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '7 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-NS2','NRT','SIN',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',530+(RANDOM()*120)::INT,'USD',220),
        (v_fid,'BUSINESS',2600+(RANDOM()*500)::INT,'USD',40),
        (v_fid,'FIRST',6200+(RANDOM()*800)::INT,'USD',20);

    -- SIN → NRT  18:00 UTC (= 02:00 SGT next day), arrives 01:00 UTC+1
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 18:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '7 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-SN2','SIN','NRT',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',540+(RANDOM()*120)::INT,'USD',220),
        (v_fid,'BUSINESS',2650+(RANDOM()*500)::INT,'USD',40),
        (v_fid,'FIRST',6300+(RANDOM()*800)::INT,'USD',20);

    -- SYD → SIN  01:00 UTC (= 11:00 AEST), arrives 08:00 UTC
    -- Then SIN → DEL 15:00 UTC = 7h layover ✓
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 01:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '7 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-YS3','SYD','SIN',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',430+(RANDOM()*150)::INT,'USD',220),
        (v_fid,'BUSINESS',2200+(RANDOM()*400)::INT,'USD',40),
        (v_fid,'FIRST',5400+(RANDOM()*800)::INT,'USD',20);

    -- JFK → SIN  00:00 UTC (= 20:00 EDT prev day), arrives 17:30 UTC (17.5h)
    -- Then SIN → DEL: need dep 19:00+ UTC (use the 17:00 UTC one we added above = too early)
    -- Add SIN→DEL 19:30 UTC (= 03:30 SGT next day), arrives 01:00 UTC+1
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 19:30:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '5 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-SD4','SIN','DEL',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',26500+(RANDOM()*8000)::INT,'INR',230),
        (v_fid,'BUSINESS',87000+(RANDOM()*20000)::INT,'INR',36),
        (v_fid,'FIRST',193000+(RANDOM()*50000)::INT,'INR',14);

    -- LHR → SIN  10:00 UTC, arrives 23:00 UTC (13h)
    -- Gives LHR→DEL via SIN: arr 23:00 UTC, need SIN→DEL next day
    -- Add DEL → LHR via SIN: DEL→SIN 00:00 UTC (05:30 IST), arr 07:00 UTC → LHR 09:00 UTC = 2h layover
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 00:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '5 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-DS2','DEL','SIN',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',26000+(RANDOM()*7000)::INT,'INR',230),
        (v_fid,'BUSINESS',86000+(RANDOM()*18000)::INT,'INR',36),
        (v_fid,'FIRST',190000+(RANDOM()*45000)::INT,'INR',14);

    -- SIN → LHR  09:00 UTC (= 17:00 SGT), arrives 16:00 UTC (7h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 09:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '13 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007',
           'SQ-'||TO_CHAR(v_day,'MMDD')||'-SL2','SIN','LHR',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',750+(RANDOM()*200)::INT,'USD',220),
        (v_fid,'BUSINESS',3800+(RANDOM()*800)::INT,'USD',36),
        (v_fid,'FIRST',9500+(RANDOM()*2000)::INT,'USD',14);

END LOOP;
END $$;

SELECT 'flights' AS tbl, COUNT(*) FROM flights
UNION ALL SELECT 'fare_classes', COUNT(*) FROM fare_classes
UNION ALL SELECT 'routes', COUNT(DISTINCT origin_iata||destination_iata) FROM flights;