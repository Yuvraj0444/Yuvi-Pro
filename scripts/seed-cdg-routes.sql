-- Add CDG↔LHR and CDG↔DXB to enable CDG→DEL and other European connections
DO $$
DECLARE
    v_day  DATE;
    v_fid  UUID;
    v_dep  TIMESTAMPTZ;
    v_arr  TIMESTAMPTZ;
    v_today DATE := CURRENT_DATE;
BEGIN
FOR v_day IN SELECT generate_series(v_today, v_today + INTERVAL '60 days', '1 day')::date LOOP

    -- CDG → LHR  07:00 UTC (= 09:00 CEST), arrives 08:30 UTC (1.5h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 07:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '1 hour 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000008','BA-'||TO_CHAR(v_day,'MMDD')||'-CL1','CDG','LHR',v_dep,v_arr,'SCHEDULED',180,180)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',120+(RANDOM()*60)::INT,'USD',140),
        (v_fid,'BUSINESS',480+(RANDOM()*120)::INT,'USD',28),
        (v_fid,'FIRST',1100+(RANDOM()*300)::INT,'USD',12);

    -- CDG → LHR  14:00 UTC, arrives 15:30 UTC
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 14:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '1 hour 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000008','BA-'||TO_CHAR(v_day,'MMDD')||'-CL2','CDG','LHR',v_dep,v_arr,'SCHEDULED',180,180)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',125+(RANDOM()*60)::INT,'USD',140),
        (v_fid,'BUSINESS',490+(RANDOM()*120)::INT,'USD',28),
        (v_fid,'FIRST',1150+(RANDOM()*300)::INT,'USD',12);

    -- LHR → CDG  09:00 UTC, arrives 10:30 UTC
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 09:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '1 hour 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000008','BA-'||TO_CHAR(v_day,'MMDD')||'-LC1','LHR','CDG',v_dep,v_arr,'SCHEDULED',180,180)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',118+(RANDOM()*60)::INT,'USD',140),
        (v_fid,'BUSINESS',475+(RANDOM()*120)::INT,'USD',28),
        (v_fid,'FIRST',1080+(RANDOM()*300)::INT,'USD',12);

    -- LHR → CDG  17:00 UTC, arrives 18:30 UTC
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 17:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '1 hour 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000008','BA-'||TO_CHAR(v_day,'MMDD')||'-LC2','LHR','CDG',v_dep,v_arr,'SCHEDULED',180,180)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',122+(RANDOM()*60)::INT,'USD',140),
        (v_fid,'BUSINESS',480+(RANDOM()*120)::INT,'USD',28),
        (v_fid,'FIRST',1100+(RANDOM()*300)::INT,'USD',12);

    -- CDG → DXB  08:00 UTC (= 10:00 CEST), arrives 15:30 UTC (7.5h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 08:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '7 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-CD1','CDG','DXB',v_dep,v_arr,'SCHEDULED',360,360)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',550+(RANDOM()*150)::INT,'USD',290),
        (v_fid,'BUSINESS',2800+(RANDOM()*600)::INT,'USD',50),
        (v_fid,'FIRST',7500+(RANDOM()*1500)::INT,'USD',20);

    -- DXB → CDG  16:00 UTC (= 20:00 GST), arrives 23:30 UTC (7.5h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 16:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '7 hours 30 minutes';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000004','EK-'||TO_CHAR(v_day,'MMDD')||'-DC1','DXB','CDG',v_dep,v_arr,'SCHEDULED',360,360)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',560+(RANDOM()*150)::INT,'USD',290),
        (v_fid,'BUSINESS',2850+(RANDOM()*600)::INT,'USD',50),
        (v_fid,'FIRST',7600+(RANDOM()*1500)::INT,'USD',20);

    -- JFK → DEL  via DXB: JFK→DXB exists, DXB→DEL exists.
    -- JFK→DXB check: need to verify timing
    -- Add JFK→SIN  22:00 UTC (= 18:00 EDT), arrives 17:00 UTC+1 (19h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 22:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '19 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-JS1','JFK','SIN',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',850+(RANDOM()*200)::INT,'USD',220),
        (v_fid,'BUSINESS',4200+(RANDOM()*800)::INT,'USD',40),
        (v_fid,'FIRST',10500+(RANDOM()*2000)::INT,'USD',20);

    -- SIN → JFK  11:00 UTC (= 19:00 SGT), arrives 08:00 UTC+1 (21h)
    v_fid := gen_random_uuid();
    v_dep := (v_day::TEXT || ' 11:00:00+00')::TIMESTAMPTZ;
    v_arr := v_dep + INTERVAL '21 hours';
    INSERT INTO flights(flight_id,airline_id,flight_number,origin_iata,destination_iata,departure_time,arrival_time,status,total_seats,available_seats)
    VALUES(v_fid,'b0000000-0000-0000-0000-000000000007','SQ-'||TO_CHAR(v_day,'MMDD')||'-SJ1','SIN','JFK',v_dep,v_arr,'SCHEDULED',280,280)
    ON CONFLICT DO NOTHING;
    INSERT INTO fare_classes(flight_id,class_type,base_price,currency,available) VALUES
        (v_fid,'ECONOMY',870+(RANDOM()*200)::INT,'USD',220),
        (v_fid,'BUSINESS',4300+(RANDOM()*800)::INT,'USD',40),
        (v_fid,'FIRST',10800+(RANDOM()*2000)::INT,'USD',20);

END LOOP;
END $$;

SELECT 'flights' AS tbl, COUNT(*) FROM flights
UNION ALL SELECT 'fare_classes', COUNT(*) FROM fare_classes
UNION ALL SELECT 'routes', COUNT(DISTINCT origin_iata||destination_iata) FROM flights;