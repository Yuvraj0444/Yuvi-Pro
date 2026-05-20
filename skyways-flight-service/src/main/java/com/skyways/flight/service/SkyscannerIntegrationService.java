package com.skyways.flight.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skyways.common.exception.flight.SkyscannerAPIException;
import com.skyways.common.security.SecretManagerService;
import com.skyways.flight.dto.FlightDto;
import com.skyways.flight.dto.FlightSearchRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integrates with the Skyscanner API via sky-scrapper.p.rapidapi.com.
 *
 * Setup (one-time):
 *   1. Go to rapidapi.com → search "sky-scrapper"
 *   2. Subscribe to the FREE plan (100 req/month)
 *   3. Your existing SKYSCANNER_API_KEY in secrets.env will work — RapidAPI uses
 *      one key across all subscribed APIs on the same account.
 *
 * Search flow:
 *   Step 1 — GET /api/v1/flights/searchAirport  → resolve IATA → skyId + entityId
 *             (cached in-memory; most airports only need one lookup)
 *   Step 2 — GET /api/v1/flights/searchFlights  → real flight results
 */
@Service
public class SkyscannerIntegrationService {

    private static final Logger log = LogManager.getLogger(SkyscannerIntegrationService.class);

    private static final String RAPIDAPI_HOST    = "sky-scrapper.p.rapidapi.com";
    private static final String BASE_URL         = "https://sky-scrapper.p.rapidapi.com";
    private static final String AIRPORT_SEARCH   = BASE_URL + "/api/v1/flights/searchAirport";
    private static final String FLIGHT_SEARCH    = BASE_URL + "/api/v1/flights/searchFlights";

    private final RestTemplate         restTemplate;
    private final SecretManagerService secretManagerService;

    /** Cache IATA → (skyId, entityId) so we only call searchAirport once per airport. */
    private final Map<String, SkyAirport> airportCache = new ConcurrentHashMap<>();

    public SkyscannerIntegrationService(RestTemplate restTemplate,
                                         SecretManagerService secretManagerService) {
        this.restTemplate         = restTemplate;
        this.secretManagerService = secretManagerService;
    }

    @CircuitBreaker(name = "skyscanner-cb", fallbackMethod = "skyscannerFallback")
    @Retry(name = "skyscanner-retry")
    public CompletableFuture<List<FlightDto>> searchFlightsAsync(FlightSearchRequest req) {
        return CompletableFuture.supplyAsync(() -> searchFlights(req));
    }

    public List<FlightDto> searchFlights(FlightSearchRequest req) {
        String origin = req.getOrigin().toUpperCase();
        String dest   = req.getDestination().toUpperCase();
        try {
            String apiKey = secretManagerService.getSecret("SKYSCANNER_API_KEY");

            // Step 1: resolve airport entity IDs (needed by sky-scrapper)
            SkyAirport originAirport = resolveAirport(origin, apiKey);
            SkyAirport destAirport   = resolveAirport(dest,   apiKey);

            if (originAirport == null || destAirport == null) {
                log.warn("Skyscanner: could not resolve airport IDs for {}->{}", origin, dest);
                return Collections.emptyList();
            }

            log.debug("Skyscanner airports resolved: {}={}/{} {}={}/{}",
                origin, originAirport.skyId, originAirport.entityId,
                dest,   destAirport.skyId,   destAirport.entityId);

            // Step 2: search flights
            return doFlightSearch(originAirport, destAirport, req, apiKey);

        } catch (HttpClientErrorException.Forbidden e) {
            log.warn("Skyscanner API key not subscribed to sky-scrapper on RapidAPI. " +
                     "Go to rapidapi.com → search 'sky-scrapper' → subscribe (free plan).");
            return Collections.emptyList();
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Skyscanner rate limit hit (429) for {}->{}. Monthly quota may be exhausted.", origin, dest);
            return Collections.emptyList();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new SkyscannerAPIException("Invalid SKYSCANNER_API_KEY — check secrets.env", e);
        } catch (Exception e) {
            log.warn("Skyscanner search failed for {}->{}: {}", origin, dest, e.getMessage());
            return Collections.emptyList();
        }
    }

    public CompletableFuture<List<FlightDto>> skyscannerFallback(FlightSearchRequest req, Throwable t) {
        log.warn("Skyscanner circuit breaker open for {}->{}: {}", req.getOrigin(), req.getDestination(), t.getMessage());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    // ── Step 1: Airport entity ID resolution ─────────────────────────────────

    private SkyAirport resolveAirport(String iata, String apiKey) {
        SkyAirport cached = airportCache.get(iata);
        if (cached != null) return cached;

        try {
            HttpHeaders  headers = buildHeaders(apiKey);
            String       url     = AIRPORT_SEARCH + "?query=" + iata + "&locale=en-US";

            ResponseEntity<SkyAirportSearchResp> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), SkyAirportSearchResp.class);

            if (resp.getBody() == null || resp.getBody().data == null || resp.getBody().data.isEmpty()) {
                log.warn("Skyscanner airport search returned no results for: {}", iata);
                return null;
            }

            log.debug("Skyscanner airport search raw for {}: {} results", iata, resp.getBody().data.size());

            // Prefer an exact IATA match; fall back to first result
            SkyAirport airport = resp.getBody().data.stream()
                .filter(a -> iata.equalsIgnoreCase(a.skyId)
                    || (a.navigation != null
                        && a.navigation.relevantFlightParams != null
                        && iata.equalsIgnoreCase(a.navigation.relevantFlightParams.skyId)))
                .findFirst()
                .orElse(resp.getBody().data.get(0));

            if (airport.entityId == null || airport.entityId.isBlank()) {
                // entityId sometimes lives inside navigation
                if (airport.navigation != null && airport.navigation.entityId != null) {
                    airport.entityId = airport.navigation.entityId;
                }
            }

            log.info("Skyscanner airport resolved: {} → skyId={} entityId={}", iata, airport.skyId, airport.entityId);
            airportCache.put(iata, airport);
            return airport;

        } catch (Exception e) {
            log.warn("Skyscanner airport lookup failed for {}: {}", iata, e.getMessage());
            return null;
        }
    }

    // ── Step 2: Flight search ─────────────────────────────────────────────────

    private List<FlightDto> doFlightSearch(SkyAirport origin, SkyAirport dest,
                                            FlightSearchRequest req, String apiKey) {
        String cabinClass = toCabinClass(req.getCabinClass());
        String url = FLIGHT_SEARCH
            + "?originSkyId="      + origin.skyId
            + "&destinationSkyId=" + dest.skyId
            + "&originEntityId="   + origin.entityId
            + "&destinationEntityId=" + dest.entityId
            + "&date="             + req.getDepartureDate()
            + "&adults="           + req.getPassengers()
            + "&currency=USD"
            + "&locale=en-US"
            + "&market=US"
            + "&cabinClass="       + cabinClass;

        log.debug("Skyscanner flight search URL: {}", url);

        ResponseEntity<SkyFlightSearchResp> resp = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(buildHeaders(apiKey)), SkyFlightSearchResp.class);

        if (resp.getBody() == null) {
            log.info("Skyscanner: null response body for {}→{}", req.getOrigin(), req.getDestination());
            return Collections.emptyList();
        }

        log.debug("Skyscanner raw response status={} hasData={}",
            resp.getBody().status,
            resp.getBody().data != null);

        return mapResponse(resp.getBody(), req);
    }

    // ── Response mapping ──────────────────────────────────────────────────────

    private List<FlightDto> mapResponse(SkyFlightSearchResp resp, FlightSearchRequest req) {
        if (resp.data == null || resp.data.itineraries == null) {
            log.info("Skyscanner: data or itineraries null in response");
            return Collections.emptyList();
        }

        String cabinClass = req.getCabinClass() != null ? req.getCabinClass().toUpperCase() : "ECONOMY";
        List<FlightDto> results = new ArrayList<>();

        for (SkyItinerary itin : resp.data.itineraries) {
            try {
                if (itin.legs == null || itin.legs.isEmpty()) continue;
                SkyLeg leg = itin.legs.get(0);

                String originIata  = leg.origin      != null ? leg.origin.displayCode      : req.getOrigin();
                String destIata    = leg.destination  != null ? leg.destination.displayCode : req.getDestination();
                String originCity  = leg.origin      != null ? leg.origin.city              : null;
                String destCity    = leg.destination  != null ? leg.destination.city         : null;

                Instant dep = parseDateTime(leg.departure);
                Instant arr = parseDateTime(leg.arrival);
                if (arr == null || arr.equals(Instant.EPOCH)) {
                    arr = dep.plusSeconds(leg.durationInMinutes * 60L);
                }

                String airlineIata = "";
                String airlineName = "Unknown";
                if (leg.carriers != null && leg.carriers.marketing != null && !leg.carriers.marketing.isEmpty()) {
                    SkyCarrier carrier = leg.carriers.marketing.get(0);
                    airlineIata = carrier.alternateId != null ? carrier.alternateId : "";
                    airlineName = carrier.name        != null ? carrier.name        : airlineIata;
                }

                BigDecimal price = BigDecimal.ZERO;
                if (itin.price != null && itin.price.raw > 0) {
                    price = BigDecimal.valueOf(itin.price.raw);
                }

                int stops = leg.stopCount;
                String connectionVia  = null;
                String connectionCity = null;
                if (stops > 0 && leg.segments != null && leg.segments.size() > 1) {
                    SkySegment midSeg = leg.segments.get(0);
                    if (midSeg.destination != null) {
                        connectionVia  = midSeg.destination.flightPlaceId;
                        connectionCity = midSeg.destination.name;
                    }
                }

                FlightDto.FlightDtoBuilder builder = FlightDto.builder()
                    .flightId(UUID.randomUUID())
                    .flightNumber(airlineIata + "-" + Math.abs(itin.id != null ? itin.id.hashCode() % 9999 : 0))
                    .airlineIata(airlineIata)
                    .airlineName(airlineName)
                    .originIata(originIata)
                    .originCity(originCity)
                    .destinationIata(destIata)
                    .destinationCity(destCity)
                    .departureTime(dep)
                    .arrivalTime(arr)
                    .durationMinutes(leg.durationInMinutes)
                    .availableSeats(9)
                    .basePrice(price)
                    .currency("USD")
                    .cabinClass(cabinClass)
                    .source("SKYSCANNER")
                    .stops(stops);

                if (stops > 0 && connectionVia != null) {
                    builder.connectionVia(connectionVia).connectionCity(connectionCity).layoverMinutes(0);
                }

                results.add(builder.build());

            } catch (Exception e) {
                log.debug("Skyscanner: skipping itinerary due to mapping error: {}", e.getMessage());
            }
        }

        log.info("Skyscanner: mapped {} itineraries from {} results for {}->{}",
            results.size(),
            resp.data.itineraries.size(),
            req.getOrigin(), req.getDestination());

        return results;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders h = new HttpHeaders();
        h.set("x-rapidapi-key",  apiKey);
        h.set("x-rapidapi-host", RAPIDAPI_HOST);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        return h;
    }

    private Instant parseDateTime(String dt) {
        if (dt == null || dt.isBlank()) return Instant.EPOCH;
        try {
            // sky-scrapper returns "2026-06-01T08:30:00" (local, no offset)
            return LocalDateTime.parse(dt).toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            try { return Instant.parse(dt); } catch (Exception ex) { return Instant.EPOCH; }
        }
    }

    private String toCabinClass(String cabin) {
        if (cabin == null) return "economy";
        return switch (cabin.toUpperCase()) {
            case "BUSINESS" -> "business";
            case "FIRST"    -> "first";
            case "PREMIUM"  -> "premiumeconomy";
            default         -> "economy";
        };
    }

    // ── Response model — sky-scrapper.p.rapidapi.com ─────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyAirportSearchResp {
        public boolean          status;
        public List<SkyAirport> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyAirport {
        public String            skyId;
        public String            entityId;
        public SkyNavigation     navigation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyNavigation {
        public String                entityId;
        public String                entityType;
        public SkyFlightParams       relevantFlightParams;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyFlightParams {
        public String skyId;
        public String entityId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyFlightSearchResp {
        public boolean     status;
        public SkyData     data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyData {
        public List<SkyItinerary> itineraries;
        public SkyContext         context;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyContext {
        public String status;
        public int    totalResults;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyItinerary {
        public String   id;
        public SkyPrice price;
        public List<SkyLeg> legs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyPrice {
        public double raw;
        public String formatted;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyLeg {
        public SkyPlace         origin;
        public SkyPlace         destination;
        public int              durationInMinutes;
        public int              stopCount;
        public String           departure;
        public String           arrival;
        public int              timeDeltaInDays;
        public SkyCarriers      carriers;
        public List<SkySegment> segments;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyPlace {
        public String id;
        public String entityId;
        public String name;
        public String displayCode;
        public String city;
        public String country;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyCarriers {
        public List<SkyCarrier> marketing;
        public List<SkyCarrier> operating;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkyCarrier {
        public int    id;
        public String alternateId;
        @JsonProperty("logoUrl")
        public String logoUrl;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkySegment {
        public SkySegmentPoint origin;
        public SkySegmentPoint destination;
        public String          departure;
        public String          arrival;
        public int             durationInMinutes;
        public String          flightNumber;
        public SkyCarrier      marketingCarrier;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SkySegmentPoint {
        public String flightPlaceId;
        public String displayCode;
        public String name;
        public String type;
        public String country;
    }
}