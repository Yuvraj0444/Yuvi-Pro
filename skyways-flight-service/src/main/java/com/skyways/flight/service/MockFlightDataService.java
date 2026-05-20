package com.skyways.flight.service;

import com.skyways.flight.dto.FlightDto;
import com.skyways.flight.dto.FlightSearchRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates realistic simulated flight data for any global route.
 * Replace calls to this class with real API integrations (Amadeus, Skyscanner, etc.)
 * when credentials are available — the FlightAggregatorService wiring stays unchanged.
 */
@Service
public class MockFlightDataService {

    private static final Logger log = LogManager.getLogger(MockFlightDataService.class);

    // ── Airport registry ──────────────────────────────────────────────────────

    private static final Map<String, Airport> AIRPORTS = new LinkedHashMap<>();

    static {
        // India — major hubs
        add("DEL", "Indira Gandhi International", "New Delhi",      "India",  28.5561,  77.1000);
        add("BOM", "Chhatrapati Shivaji International", "Mumbai",   "India",  19.0896,  72.8656);
        add("BLR", "Kempegowda International", "Bengaluru",         "India",  13.1979,  77.7063);
        add("MAA", "Chennai International", "Chennai",              "India",  12.9900,  80.1693);
        add("HYD", "Rajiv Gandhi International", "Hyderabad",       "India",  17.2403,  78.4294);
        add("CCU", "Netaji Subhas Chandra Bose", "Kolkata",         "India",  22.6547,  88.4467);
        add("COK", "Cochin International", "Kochi",                 "India",   9.9952,  76.2698);
        add("AMD", "Sardar Vallabhbhai Patel", "Ahmedabad",         "India",  23.0772,  72.6347);
        add("PNQ", "Pune Airport", "Pune",                         "India",  18.5822,  73.9197);
        add("GOI", "Goa International", "Goa",                     "India",  15.3808,  73.8314);
        // India — secondary cities
        add("JAI", "Jaipur International", "Jaipur",               "India",  26.8242,  75.8122);
        add("LKO", "Chaudhary Charan Singh International", "Lucknow", "India", 26.7606, 80.8893);
        add("IXC", "Shaheed Bhagat Singh International", "Chandigarh", "India", 30.6735, 76.7885);
        add("VNS", "Lal Bahadur Shastri International", "Varanasi", "India", 25.4524,  82.8593);
        add("PAT", "Jay Prakash Narayan International", "Patna",   "India",  25.5913,  85.0880);
        add("BBI", "Biju Patnaik International", "Bhubaneswar",    "India",  20.2444,  85.8178);
        add("TRV", "Trivandrum International", "Thiruvananthapuram","India",   8.4821,  76.9202);
        add("GAU", "Lokpriya Gopinath Bordoloi", "Guwahati",       "India",  26.1061,  91.5859);
        add("ATQ", "Sri Guru Ram Dass Jee International", "Amritsar","India", 31.7096,  74.7973);
        add("IXM", "Madurai Airport", "Madurai",                   "India",   9.8345,  78.0934);
        add("SXR", "Sheikh ul-Alam International", "Srinagar",     "India",  33.9871,  74.7742);
        add("UDR", "Maharana Pratap Airport", "Udaipur",           "India",  24.6177,  73.8961);
        add("RPR", "Swami Vivekananda Airport", "Raipur",          "India",  21.1804,  81.7388);
        add("BDQ", "Vadodara Airport", "Vadodara",                 "India",  22.3362,  73.2263);
        add("VGA", "Vijayawada Airport", "Vijayawada",             "India",  16.5304,  80.7968);
        add("IXB", "Bagdogra Airport", "Siliguri",                 "India",  26.6812,  88.3286);
        add("IXR", "Birsa Munda Airport", "Ranchi",                "India",  23.3143,  85.3217);
        // Middle East
        add("DXB", "Dubai International", "Dubai",                 "UAE",           25.2528,  55.3644);
        add("AUH", "Abu Dhabi International", "Abu Dhabi",         "UAE",           24.4330,  54.6511);
        add("DOH", "Hamad International", "Doha",                  "Qatar",         25.2608,  51.6138);
        add("RUH", "King Khalid International", "Riyadh",          "Saudi Arabia",  24.9597,  46.6987);
        add("KWI", "Kuwait International", "Kuwait City",          "Kuwait",        29.2267,  47.9689);
        add("MCT", "Muscat International", "Muscat",               "Oman",          23.5933,  58.2844);
        // Europe
        add("LHR", "Heathrow", "London",                          "UK",            51.4775,  -0.4614);
        add("CDG", "Charles de Gaulle", "Paris",                   "France",        49.0097,   2.5479);
        add("FRA", "Frankfurt Airport", "Frankfurt",               "Germany",       50.0379,   8.5622);
        add("AMS", "Amsterdam Schiphol", "Amsterdam",              "Netherlands",   52.3086,   4.7639);
        add("MAD", "Adolfo Suárez Madrid–Barajas", "Madrid",       "Spain",         40.4936,  -3.5668);
        add("FCO", "Leonardo da Vinci–Fiumicino", "Rome",          "Italy",         41.8003,  12.2388);
        add("ZUR", "Zurich Airport", "Zurich",                     "Switzerland",   47.4647,   8.5492);
        add("IST", "Istanbul Airport", "Istanbul",                 "Turkey",        41.2608,  28.7418);
        add("VIE", "Vienna International", "Vienna",               "Austria",       48.1103,  16.5697);
        add("CPH", "Copenhagen Airport", "Copenhagen",             "Denmark",       55.6181,  12.6560);
        // North America
        add("JFK", "John F Kennedy International", "New York",     "USA",           40.6413, -73.7781);
        add("LAX", "Los Angeles International", "Los Angeles",     "USA",           33.9425,-118.4081);
        add("ORD", "O'Hare International", "Chicago",              "USA",           41.9742, -87.9073);
        add("SFO", "San Francisco International", "San Francisco", "USA",           37.6213,-122.3790);
        add("YYZ", "Toronto Pearson International", "Toronto",     "Canada",        43.6777, -79.6248);
        add("YVR", "Vancouver International", "Vancouver",         "Canada",        49.1947,-123.1792);
        add("MIA", "Miami International", "Miami",                 "USA",           25.7959, -80.2870);
        add("BOS", "Logan International", "Boston",                "USA",           42.3656, -71.0096);
        // East Asia
        add("SIN", "Changi Airport", "Singapore",                  "Singapore",      1.3644, 103.9915);
        add("BKK", "Suvarnabhumi Airport", "Bangkok",              "Thailand",      13.6900, 100.7501);
        add("KUL", "Kuala Lumpur International", "Kuala Lumpur",   "Malaysia",       2.7456, 101.7099);
        add("HKG", "Hong Kong International", "Hong Kong",         "Hong Kong",     22.3080, 113.9185);
        add("NRT", "Narita International", "Tokyo",                "Japan",         35.7720, 140.3929);
        add("ICN", "Incheon International", "Seoul",               "South Korea",   37.4602, 126.4407);
        add("PVG", "Pudong International", "Shanghai",             "China",         31.1443, 121.8083);
        add("SYD", "Kingsford Smith", "Sydney",                    "Australia",    -33.9399, 151.1753);
        add("MEL", "Melbourne Airport", "Melbourne",               "Australia",    -37.6690, 144.8410);
        // Africa
        add("NBO", "Jomo Kenyatta International", "Nairobi",       "Kenya",         -1.3192,  36.9275);
        add("JNB", "OR Tambo International", "Johannesburg",       "South Africa", -26.1392,  28.2460);
        add("CAI", "Cairo International", "Cairo",                 "Egypt",         30.1219,  31.4056);
        add("CMN", "Mohammed V International", "Casablanca",       "Morocco",       33.3675,  -7.5898);
        add("LOS", "Murtala Muhammed International", "Lagos",      "Nigeria",        6.5774,   3.3211);
        add("ADD", "Bole International", "Addis Ababa",            "Ethiopia",       8.9779,  38.7993);
        // Latin America
        add("GRU", "São Paulo–Guarulhos", "São Paulo",             "Brazil",       -23.4356, -46.4731);
        add("EZE", "Ministro Pistarini", "Buenos Aires",           "Argentina",    -34.8222, -58.5358);
        add("BOG", "El Dorado International", "Bogotá",            "Colombia",       4.7016, -74.1469);
        add("MEX", "Mexico City International", "Mexico City",     "Mexico",        19.4363, -99.0721);
        add("SCL", "Arturo Merino Benítez", "Santiago",            "Chile",        -33.3930, -70.7858);
        // South Asia
        add("KTM", "Tribhuvan International", "Kathmandu",         "Nepal",         27.6966,  85.3591);
        add("CMB", "Bandaranaike International", "Colombo",        "Sri Lanka",      7.1808,  79.8841);
        add("DAC", "Hazrat Shahjalal International", "Dhaka",      "Bangladesh",    23.8433,  90.3978);
        add("KHI", "Jinnah International", "Karachi",              "Pakistan",      24.9065,  67.1608);
    }

    private static void add(String iata, String name, String city, String country, double lat, double lon) {
        AIRPORTS.put(iata.toUpperCase(), new Airport(iata.toUpperCase(), name, city, country, lat, lon));
    }

    // ── Airlines with regional preference weights ─────────────────────────────

    private static final List<Airline> AIRLINES = List.of(
        new Airline("AI",  "Air India",           "India"),
        new Airline("6E",  "IndiGo",              "India"),
        new Airline("SG",  "SpiceJet",            "India"),
        new Airline("UK",  "Vistara",             "India"),
        new Airline("EK",  "Emirates",            "UAE"),
        new Airline("EY",  "Etihad Airways",      "UAE"),
        new Airline("QR",  "Qatar Airways",       "Qatar"),
        new Airline("SV",  "Saudia",              "Saudi Arabia"),
        new Airline("BA",  "British Airways",     "UK"),
        new Airline("LH",  "Lufthansa",           "Germany"),
        new Airline("AF",  "Air France",          "France"),
        new Airline("KL",  "KLM",                 "Netherlands"),
        new Airline("IB",  "Iberia",              "Spain"),
        new Airline("TK",  "Turkish Airlines",    "Turkey"),
        new Airline("SQ",  "Singapore Airlines",  "Singapore"),
        new Airline("MH",  "Malaysia Airlines",   "Malaysia"),
        new Airline("TG",  "Thai Airways",        "Thailand"),
        new Airline("CX",  "Cathay Pacific",      "Hong Kong"),
        new Airline("NH",  "ANA",                 "Japan"),
        new Airline("OZ",  "Asiana Airlines",     "South Korea"),
        new Airline("AA",  "American Airlines",   "USA"),
        new Airline("UA",  "United Airlines",     "USA"),
        new Airline("DL",  "Delta Air Lines",     "USA"),
        new Airline("AC",  "Air Canada",          "Canada"),
        new Airline("QF",  "Qantas",              "Australia"),
        new Airline("ET",  "Ethiopian Airlines",  "Ethiopia"),
        new Airline("KQ",  "Kenya Airways",       "Kenya"),
        new Airline("LA",  "LATAM Airlines",      "Chile"),
        new Airline("G8",  "Go First",            "India"),
        new Airline("WY",  "Oman Air",            "Oman")
    );

    // ── Main entry point ──────────────────────────────────────────────────────

    public List<FlightDto> searchFlights(FlightSearchRequest req) {
        String origin = req.getOrigin().toUpperCase();
        String dest   = req.getDestination().toUpperCase();

        Airport originAirport = AIRPORTS.get(origin);
        Airport destAirport   = AIRPORTS.get(dest);

        // For unknown airports, generate plausible placeholder coordinates
        if (originAirport == null) originAirport = syntheticAirport(origin);
        if (destAirport   == null) destAirport   = syntheticAirport(dest);

        double distanceKm  = haversineKm(originAirport.lat, originAirport.lon,
                                          destAirport.lat,   destAirport.lon);
        int    durationMin = estimateDurationMin(distanceKm);
        String cabinClass  = req.getCabinClass() != null ? req.getCabinClass().toUpperCase() : "ECONOMY";

        List<Airline> pool = selectAirlines(originAirport, destAirport, distanceKm);

        List<FlightDto> flights = new ArrayList<>();
        LocalDate date = req.getDepartureDate();

        // Generate 4–6 flights spread through the day
        int[] departureHours = {6, 9, 12, 15, 18, 22};
        int flightCount = Math.min(pool.size(), departureHours.length);

        for (int i = 0; i < flightCount; i++) {
            Airline airline = pool.get(i);
            int     hour    = departureHours[i];

            Instant departure = date.atTime(hour, randomMinute(origin + dest + i))
                .toInstant(ZoneOffset.UTC);
            Instant arrival = departure.plusSeconds(durationMin * 60L);

            BigDecimal price = generatePrice(distanceKm, cabinClass, i, req.getPassengers());

            String flightNumber = airline.iata + generateFlightNum(origin, dest, hour);

            FlightDto.FlightDtoBuilder builder = FlightDto.builder()
                .flightId(deterministicUUID(flightNumber + date + origin + dest))
                .flightNumber(flightNumber)
                .airlineIata(airline.iata)
                .airlineName(airline.name)
                .originIata(originAirport.iata)
                .originCity(originAirport.city)
                .destinationIata(destAirport.iata)
                .destinationCity(destAirport.city)
                .departureTime(departure)
                .arrivalTime(arrival)
                .durationMinutes(durationMin)
                .availableSeats(randomSeats(origin + dest + airline.iata + i))
                .basePrice(price)
                .currency("INR")
                .cabinClass(cabinClass)
                .source("SIMULATED")
                .stops(0);

            flights.add(builder.build());
        }

        // Add one connecting flight for long-haul routes (>5000 km)
        if (distanceKm > 5000 && !pool.isEmpty()) {
            FlightDto connecting = buildConnectingFlight(
                originAirport, destAirport, pool.get(0), distanceKm, durationMin, date, cabinClass, req.getPassengers());
            if (connecting != null) flights.add(connecting);
        }

        log.info("Mock: generated {} flights for {}->{} ({} km, ~{} min)",
            flights.size(), origin, dest, (int) distanceKm, durationMin);

        return flights.stream()
            .sorted(Comparator.comparing(FlightDto::getBasePrice))
            .collect(Collectors.toList());
    }

    // ── Connecting flight builder ──────────────────────────────────────────────

    private FlightDto buildConnectingFlight(Airport origin, Airport dest, Airline airline,
                                             double distKm, int totalDurationMin,
                                             LocalDate date, String cabinClass, int passengers) {
        // Pick a hub close to the origin as the connection point
        Airport hub = findHub(origin, dest);
        if (hub == null || hub.iata.equals(origin.iata) || hub.iata.equals(dest.iata)) return null;

        int leg1Min = (int) (totalDurationMin * 0.45);
        int layover = 90; // 90-minute layover
        int leg2Min = totalDurationMin - leg1Min;
        int totalMin = leg1Min + layover + leg2Min;

        Instant dep1 = date.atTime(7, 30).toInstant(ZoneOffset.UTC);
        Instant arr1 = dep1.plusSeconds(leg1Min * 60L);
        Instant dep2 = arr1.plusSeconds(layover * 60L);
        Instant arr2 = dep2.plusSeconds(leg2Min * 60L);

        BigDecimal price = generatePrice(distKm, cabinClass, 99, passengers)
            .multiply(BigDecimal.valueOf(0.88)).setScale(2, RoundingMode.HALF_UP);

        return FlightDto.builder()
            .flightId(deterministicUUID("conn-" + origin.iata + hub.iata + dest.iata + date))
            .flightNumber(airline.iata + generateFlightNum(origin.iata, dest.iata, 7) + "C")
            .airlineIata(airline.iata)
            .airlineName(airline.name)
            .originIata(origin.iata)
            .originCity(origin.city)
            .destinationIata(dest.iata)
            .destinationCity(dest.city)
            .departureTime(dep1)
            .arrivalTime(arr2)
            .durationMinutes(totalMin)
            .availableSeats(randomSeats("conn-" + origin.iata + dest.iata))
            .basePrice(price)
            .currency("INR")
            .cabinClass(cabinClass)
            .source("SIMULATED")
            .stops(1)
            .connectionVia(hub.iata)
            .connectionCity(hub.city)
            .layoverMinutes(layover)
            .build();
    }

    // ── Haversine distance ────────────────────────────────────────────────────

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    private int estimateDurationMin(double km) {
        // ~900 km/h cruise + 30-min ground ops overhead
        int flight = (int) (km / 900.0 * 60);
        return Math.max(45, flight + 30);
    }

    // ── Airline selection by region ───────────────────────────────────────────

    private List<Airline> selectAirlines(Airport origin, Airport dest, double distKm) {
        List<Airline> scored = new ArrayList<>(AIRLINES);
        // Stable pseudo-shuffle seeded on route so same route always gets same airlines
        long seed = (long) origin.iata.hashCode() * 31 + dest.iata.hashCode();
        Collections.shuffle(scored, new Random(seed));

        // Boost home-country airlines to front
        scored.sort((a, b) -> {
            int scoreA = airlineScore(a, origin, dest);
            int scoreB = airlineScore(b, origin, dest);
            return Integer.compare(scoreB, scoreA);
        });

        // Domestic routes: prefer local carriers
        if (distKm < 2000) {
            scored = scored.stream()
                .filter(a -> isRegionalMatch(a, origin, dest))
                .limit(6)
                .collect(Collectors.toList());
            if (scored.size() < 4) scored.addAll(AIRLINES.subList(0, 4 - scored.size()));
        }

        return scored.stream().limit(6).collect(Collectors.toList());
    }

    private int airlineScore(Airline a, Airport origin, Airport dest) {
        int s = 0;
        if (a.country.equals(origin.country) || a.country.equals(dest.country)) s += 10;
        // Major hubs get bonus
        if (a.iata.equals("EK") && (origin.country.equals("UAE")    || dest.country.equals("UAE")))    s += 5;
        if (a.iata.equals("QR") && (origin.country.equals("Qatar")  || dest.country.equals("Qatar")))  s += 5;
        if (a.iata.equals("SQ") && (origin.iata.equals("SIN")       || dest.iata.equals("SIN")))       s += 5;
        if (a.iata.equals("TK") && (origin.country.equals("Turkey") || dest.country.equals("Turkey"))) s += 5;
        if (a.iata.equals("ET") && (origin.iata.equals("ADD")       || dest.iata.equals("ADD")))       s += 5;
        return s;
    }

    private boolean isRegionalMatch(Airline a, Airport origin, Airport dest) {
        return a.country.equals(origin.country) || a.country.equals(dest.country);
    }

    // ── Price generation (INR) ────────────────────────────────────────────────

    private BigDecimal generatePrice(double km, String cabinClass, int slot, int passengers) {
        double base;
        if      (km < 500)   base = 1500 + km * 3.0;   // ultra-short domestic: ₹1,500–₹3,000
        else if (km < 1500)  base = 2500 + km * 3.5;   // short domestic: ₹2,500–₹7,750
        else if (km < 3000)  base = 4000 + km * 4.0;   // medium domestic/regional: ₹4,000–₹16,000
        else if (km < 6000)  base = 15000 + km * 4.5;  // international: ₹15,000–₹42,000
        else                 base = 30000 + km * 3.5;  // long-haul: ₹30,000–₹75,000+

        // Cabin multiplier
        double cabinMult = switch (cabinClass) {
            case "BUSINESS"       -> 3.5;
            case "FIRST"          -> 6.0;
            case "PREMIUM_ECONOMY",
                 "PREMIUMECONOMY" -> 1.8;
            default               -> 1.0;
        };

        // Slot 0 is cheapest, higher slots are slightly pricier
        double slotMult = 1.0 + (slot % 6) * 0.06;

        double total = base * cabinMult * slotMult * passengers;
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Hub finder for connecting flights ─────────────────────────────────────

    private Airport findHub(Airport origin, Airport dest) {
        // Major international hubs preferred as layover points
        List<String> hubs = List.of("DXB","DOH","SIN","LHR","FRA","IST","AMS","NRT","BKK","JFK","EZE","NBO","ADD");
        for (String hubIata : hubs) {
            Airport hub = AIRPORTS.get(hubIata);
            if (hub == null) continue;
            // Hub should be roughly between origin and dest
            double d1 = haversineKm(origin.lat, origin.lon, hub.lat, hub.lon);
            double d2 = haversineKm(hub.lat, hub.lon, dest.lat, dest.lon);
            double direct = haversineKm(origin.lat, origin.lon, dest.lat, dest.lon);
            if (d1 + d2 < direct * 1.5 && d1 > 200 && d2 > 200) return hub;
        }
        return AIRPORTS.get("DXB");
    }

    // ── Deterministic helpers ─────────────────────────────────────────────────

    private UUID deterministicUUID(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes());
    }

    private int randomMinute(String seed) {
        return Math.abs(seed.hashCode()) % 60;
    }

    private int randomSeats(String seed) {
        int base = Math.abs(seed.hashCode()) % 120;
        return Math.max(1, base + 5);
    }

    private String generateFlightNum(String origin, String dest, int hour) {
        int num = (Math.abs((origin + dest).hashCode()) % 8000) + 1000 + hour;
        return String.valueOf(num);
    }

    private Airport syntheticAirport(String iata) {
        // Assign plausible coordinates based on hash so unknown airports still work
        double lat = (Math.abs(iata.hashCode()) % 160) - 80.0;
        double lon = (Math.abs(iata.hashCode() * 31) % 360) - 180.0;
        return new Airport(iata, iata + " Airport", iata, "Unknown", lat, lon);
    }

    // ── Inner records ─────────────────────────────────────────────────────────

    private record Airport(String iata, String name, String city, String country, double lat, double lon) {}
    private record Airline(String iata, String name, String country) {}
}