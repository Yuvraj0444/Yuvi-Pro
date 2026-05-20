-- Add extra daily departure times for hub routes to enable 1-stop connections
DO $$
DECLARE
    v_day  DATE;
    v_dep  TIMESTAMPTZ;
    v_arr  TIMESTAMPTZ;
    v_fid  UUID;
    v_today DATE := CURRENT_DATE;
BEGIN
FOR v_day IN SELECT generate_series(v_today, v_today + INTERVAL '60 days', '1 day')::date LOOP

    -- SIN -> DEL  (second departure: 18:00 SGT = 10:00 UTC)
    v_fid := gen_random_uuid();
    v_dep := (v_day + TIME '18:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Singapore';
    v_arr := v_dep + INTERVAL '5 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-SD2','SIN','DEL',v_dep,v_arr,'SCHEDULED',280,280);
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',27500+(RANDOM()*8000)::INT,'INR',230),
        (v_fid,'BUSINESS',89000+(RANDOM()*20000)::INT,'INR',36),
        (v_fid,'FIRST',196000+(RANDOM()*50000)::INT,'INR',14);

    -- SIN -> BOM  (2 departures: 10:00 SGT = 02:00 UTC, 20:00 SGT = 12:00 UTC)
    FOREACH v_dep IN ARRAY ARRAY[
        (v_day + TIME '10:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Singapore',
        (v_day + TIME '20:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Singapore'
    ] LOOP
        v_fid := gen_random_uuid();
        v_arr := v_dep + INTERVAL '5 hours';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-SB'||EXTRACT(HOUR FROM v_dep AT TIME ZONE 'UTC')::INT,'SIN','BOM',v_dep,v_arr,'SCHEDULED',280,280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',25000+(RANDOM()*7000)::INT,'INR',230),
            (v_fid,'BUSINESS',85000+(RANDOM()*18000)::INT,'INR',36),
            (v_fid,'FIRST',185000+(RANDOM()*45000)::INT,'INR',14);
    END LOOP;

    -- BOM -> SIN  (10:00 IST = 04:30 UTC, 23:30 IST = 18:00 UTC)
    FOREACH v_dep IN ARRAY ARRAY[
        (v_day + TIME '10:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata',
        (v_day + TIME '23:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'
    ] LOOP
        v_fid := gen_random_uuid();
        v_arr := v_dep + INTERVAL '5 hours 30 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-BS'||EXTRACT(HOUR FROM v_dep AT TIME ZONE 'UTC')::INT,'BOM','SIN',v_dep,v_arr,'SCHEDULED',280,280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',24000+(RANDOM()*7000)::INT,'INR',230),
            (v_fid,'BUSINESS',82000+(RANDOM()*18000)::INT,'INR',36),
            (v_fid,'FIRST',182000+(RANDOM()*45000)::INT,'INR',14);
    END LOOP;

    -- LHR -> DEL  (second departure: 09:00 BST = 08:00 UTC)
    v_fid := gen_random_uuid();
    v_dep := (v_day + TIME '09:00')::TIMESTAMPTZ AT TIME ZONE 'Europe/London';
    v_arr := v_dep + INTERVAL '8 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000008','BA-'||TO_CHAR(v_day,'MMDD')||'-LD2','LHR','DEL',v_dep,v_arr,'SCHEDULED',280,280);
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',45000+(RANDOM()*14000)::INT,'INR',220),
        (v_fid,'BUSINESS',147000+(RANDOM()*50000)::INT,'INR',40),
        (v_fid,'FIRST',342000+(RANDOM()*80000)::INT,'INR',20);

    -- DXB -> LHR  (06:00 GST = 02:00 UTC, 14:00 GST = 10:00 UTC)
    FOREACH v_dep IN ARRAY ARRAY[
        (v_day + TIME '06:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Dubai',
        (v_day + TIME '14:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Dubai'
    ] LOOP
        v_fid := gen_random_uuid();
        v_arr := v_dep + INTERVAL '7 hours';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-DL'||EXTRACT(HOUR FROM v_dep AT TIME ZONE 'UTC')::INT,'DXB','LHR',v_dep,v_arr,'SCHEDULED',360,360);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',600+(RANDOM()*200)::INT,'USD',290),
            (v_fid,'BUSINESS',3200+(RANDOM()*800)::INT,'USD',50),
            (v_fid,'FIRST',8500+(RANDOM()*2000)::INT,'USD',20);
    END LOOP;

    -- LHR -> DXB  (08:00 BST = 07:00 UTC, 20:00 BST = 19:00 UTC)
    FOREACH v_dep IN ARRAY ARRAY[
        (v_day + TIME '08:00')::TIMESTAMPTZ AT TIME ZONE 'Europe/London',
        (v_day + TIME '20:00')::TIMESTAMPTZ AT TIME ZONE 'Europe/London'
    ] LOOP
        v_fid := gen_random_uuid();
        v_arr := v_dep + INTERVAL '7 hours';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-LD'||EXTRACT(HOUR FROM v_dep AT TIME ZONE 'UTC')::INT,'LHR','DXB',v_dep,v_arr,'SCHEDULED',360,360);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',580+(RANDOM()*200)::INT,'USD',290),
            (v_fid,'BUSINESS',3100+(RANDOM()*800)::INT,'USD',50),
            (v_fid,'FIRST',8200+(RANDOM()*2000)::INT,'USD',20);
    END LOOP;

    -- NRT -> DEL  (via SIN logic: add NRT→DEL direct if none, plus better SIN times)
    -- HKG -> DEL  (06:00 HKT = 22:00 UTC prev day, 14:00 HKT = 06:00 UTC)
    FOREACH v_dep IN ARRAY ARRAY[
        (v_day + TIME '14:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Hong_Kong'
    ] LOOP
        v_fid := gen_random_uuid();
        v_arr := v_dep + INTERVAL '6 hours';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-HD'||EXTRACT(HOUR FROM v_dep AT TIME ZONE 'UTC')::INT,'HKG','DEL',v_dep,v_arr,'SCHEDULED',250,250);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',32000+(RANDOM()*8000)::INT,'INR',200),
            (v_fid,'BUSINESS',95000+(RANDOM()*20000)::INT,'INR',36),
            (v_fid,'FIRST',210000+(RANDOM()*50000)::INT,'INR',14);
    END LOOP;

    -- DEL -> HKG  (23:00 IST = 17:30 UTC)
    v_fid := gen_random_uuid();
    v_dep := (v_day + TIME '23:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata';
    v_arr := v_dep + INTERVAL '6 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-DH','DEL','HKG',v_dep,v_arr,'SCHEDULED',250,250);
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',31000+(RANDOM()*8000)::INT,'INR',200),
        (v_fid,'BUSINESS',93000+(RANDOM()*20000)::INT,'INR',36),
        (v_fid,'FIRST',205000+(RANDOM()*50000)::INT,'INR',14);

    -- NRT -> HKG  (10:00 JST = 01:00 UTC, arrives 13:30 JST = 04:30 UTC)
    v_fid := gen_random_uuid();
    v_dep := (v_day + TIME '10:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Tokyo';
    v_arr := v_dep + INTERVAL '3 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-NH','NRT','HKG',v_dep,v_arr,'SCHEDULED',250,250);
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',350+(RANDOM()*100)::INT,'USD',200),
        (v_fid,'BUSINESS',1800+(RANDOM()*400)::INT,'USD',36),
        (v_fid,'FIRST',4500+(RANDOM()*800)::INT,'USD',14);

    -- HKG -> NRT  (14:30 HKT = 06:30 UTC, arrives 19:00 HKT = 11:00 UTC)
    v_fid := gen_random_uuid();
    v_dep := (v_day + TIME '14:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Hong_Kong';
    v_arr := v_dep + INTERVAL '4 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-HN','HKG','NRT',v_dep,v_arr,'SCHEDULED',250,250);
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',360+(RANDOM()*100)::INT,'USD',200),
        (v_fid,'BUSINESS',1850+(RANDOM()*400)::INT,'USD',36),
        (v_fid,'FIRST',4600+(RANDOM()*800)::INT,'USD',14);

    -- SYD -> DEL  via Singapore (SYD→SIN 09:30 AEST=23:30 UTC, arr +8h = 07:30 UTC; SIN→DEL 18:00 SGT=10:00 UTC)
    -- Just add SYD→SIN at 22:00 AEST = 12:00 UTC too
    v_fid := gen_random_uuid();
    v_dep := (v_day + TIME '22:00')::TIMESTAMPTZ AT TIME ZONE 'Australia/Sydney';
    v_arr := v_dep + INTERVAL '8 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-YS2','SYD','SIN',v_dep,v_arr,'SCHEDULED',280,280);
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',420+(RANDOM()*150)::INT,'USD',220),
        (v_fid,'BUSINESS',2150+(RANDOM()*400)::INT,'USD',40),
        (v_fid,'FIRST',5300+(RANDOM()*800)::INT,'USD',20);

END LOOP;
END $$;

SELECT 'flights' AS tbl, COUNT(*) FROM flights
UNION ALL SELECT 'fare_classes', COUNT(*) FROM fare_classes
UNION ALL SELECT 'routes', COUNT(DISTINCT origin_iata||destination_iata) FROM flights;