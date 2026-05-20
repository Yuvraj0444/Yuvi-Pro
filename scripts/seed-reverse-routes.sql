DO $$
DECLARE
    v_day       DATE;
    v_dep_time  TIMESTAMPTZ;
    v_arr_time  TIMESTAMPTZ;
    v_fid       UUID;
    v_today     DATE := CURRENT_DATE;
BEGIN
FOR v_day IN SELECT generate_series(v_today, v_today + INTERVAL '60 days', '1 day')::date LOOP

    -- BLR -> BOM (1h 55m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '07:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata',
        (v_day + TIME '18:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '1 hour 55 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000002','6E-'||TO_CHAR(v_day,'MMDD')||'-BM'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'BLR','BOM',v_dep_time,v_arr_time,'SCHEDULED',180,180);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',2900+(RANDOM()*800)::INT,'INR',150),
            (v_fid,'BUSINESS',9800+(RANDOM()*2000)::INT,'INR',20),
            (v_fid,'FIRST',21000+(RANDOM()*4000)::INT,'INR',10);
    END LOOP;

    -- HYD -> DEL (2h 15m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '08:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata',
        (v_day + TIME '19:30')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '2 hours 15 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000001','AI-'||TO_CHAR(v_day,'MMDD')||'-HD'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'HYD','DEL',v_dep_time,v_arr_time,'SCHEDULED',200,200);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',3300+(RANDOM()*1000)::INT,'INR',165),
            (v_fid,'BUSINESS',11500+(RANDOM()*3000)::INT,'INR',24),
            (v_fid,'FIRST',23000+(RANDOM()*5000)::INT,'INR',11);
    END LOOP;

    -- DXB -> BOM (3h 10m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '10:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Dubai',
        (v_day + TIME '22:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Dubai'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '3 hours 10 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-DB'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'DXB','BOM',v_dep_time,v_arr_time,'SCHEDULED',300,300);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',16500+(RANDOM()*5000)::INT,'INR',250),
            (v_fid,'BUSINESS',51000+(RANDOM()*15000)::INT,'INR',35),
            (v_fid,'FIRST',112000+(RANDOM()*30000)::INT,'INR',15);
    END LOOP;

    -- LHR -> DEL (8h 30m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '14:00')::TIMESTAMPTZ AT TIME ZONE 'Europe/London'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '8 hours 30 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000008','BA-'||TO_CHAR(v_day,'MMDD')||'-LD'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'LHR','DEL',v_dep_time,v_arr_time,'SCHEDULED',280,280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',44000+(RANDOM()*14000)::INT,'INR',220),
            (v_fid,'BUSINESS',145000+(RANDOM()*50000)::INT,'INR',40),
            (v_fid,'FIRST',340000+(RANDOM()*80000)::INT,'INR',20);
    END LOOP;

    -- LHR -> BOM (9h)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '21:30')::TIMESTAMPTZ AT TIME ZONE 'Europe/London'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '9 hours';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000001','AI-'||TO_CHAR(v_day,'MMDD')||'-LB'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'LHR','BOM',v_dep_time,v_arr_time,'SCHEDULED',250,250);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',41000+(RANDOM()*12000)::INT,'INR',200),
            (v_fid,'BUSINESS',138000+(RANDOM()*40000)::INT,'INR',36),
            (v_fid,'FIRST',310000+(RANDOM()*70000)::INT,'INR',14);
    END LOOP;

    -- SIN -> DEL (5h 30m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '09:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Singapore'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '5 hours 30 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-SD'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'SIN','DEL',v_dep_time,v_arr_time,'SCHEDULED',280,280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',27000+(RANDOM()*8000)::INT,'INR',230),
            (v_fid,'BUSINESS',88000+(RANDOM()*20000)::INT,'INR',36),
            (v_fid,'FIRST',195000+(RANDOM()*50000)::INT,'INR',14);
    END LOOP;

    -- SYD -> SIN (8h)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '09:30')::TIMESTAMPTZ AT TIME ZONE 'Australia/Sydney'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '8 hours';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-YS'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'SYD','SIN',v_dep_time,v_arr_time,'SCHEDULED',280,280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',410+(RANDOM()*150)::INT,'USD',220),
            (v_fid,'BUSINESS',2100+(RANDOM()*400)::INT,'USD',40),
            (v_fid,'FIRST',5200+(RANDOM()*800)::INT,'USD',20);
    END LOOP;

    -- JFK -> CDG (7h 30m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '18:00')::TIMESTAMPTZ AT TIME ZONE 'America/New_York'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '7 hours 30 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000010','AF-'||TO_CHAR(v_day,'MMDD')||'-JC'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'JFK','CDG',v_dep_time,v_arr_time,'SCHEDULED',280,280);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',480+(RANDOM()*180)::INT,'USD',220),
            (v_fid,'BUSINESS',2250+(RANDOM()*500)::INT,'USD',40),
            (v_fid,'FIRST',5300+(RANDOM()*1000)::INT,'USD',20);
    END LOOP;

    -- BOM -> HYD (1h 30m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '10:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata',
        (v_day + TIME '19:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '1 hour 30 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000002','6E-'||TO_CHAR(v_day,'MMDD')||'-BH'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'BOM','HYD',v_dep_time,v_arr_time,'SCHEDULED',180,180);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',2500+(RANDOM()*800)::INT,'INR',155),
            (v_fid,'BUSINESS',8500+(RANDOM()*2000)::INT,'INR',18),
            (v_fid,'FIRST',18000+(RANDOM()*4000)::INT,'INR',7);
    END LOOP;

    -- BLR -> HYD (1h 10m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '11:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '1 hour 10 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000002','6E-'||TO_CHAR(v_day,'MMDD')||'-BY'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'BLR','HYD',v_dep_time,v_arr_time,'SCHEDULED',180,180);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',2200+(RANDOM()*600)::INT,'INR',155),
            (v_fid,'BUSINESS',7500+(RANDOM()*2000)::INT,'INR',18),
            (v_fid,'FIRST',16000+(RANDOM()*3000)::INT,'INR',7);
    END LOOP;

    -- HYD -> BLR (1h 10m)
    FOREACH v_dep_time IN ARRAY ARRAY[
        (v_day + TIME '13:00')::TIMESTAMPTZ AT TIME ZONE 'Asia/Kolkata'
    ] LOOP
        v_fid := gen_random_uuid(); v_arr_time := v_dep_time + INTERVAL '1 hour 10 minutes';
        INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
        VALUES(v_fid,'b0000000-0000-0000-0000-000000000002','6E-'||TO_CHAR(v_day,'MMDD')||'-HY'||EXTRACT(HOUR FROM v_dep_time AT TIME ZONE 'UTC')::INT,'HYD','BLR',v_dep_time,v_arr_time,'SCHEDULED',180,180);
        INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
            (v_fid,'ECONOMY',2200+(RANDOM()*600)::INT,'INR',155),
            (v_fid,'BUSINESS',7500+(RANDOM()*2000)::INT,'INR',18),
            (v_fid,'FIRST',16000+(RANDOM()*3000)::INT,'INR',7);
    END LOOP;

END LOOP;
END $$;

SELECT DISTINCT origin_iata||'->'||destination_iata AS route FROM flights ORDER BY 1;