package com.skyways.flight.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skyways.common.security.SecretManagerService;
import com.skyways.flight.dto.FlightDto;
import com.skyways.flight.dto.FlightSearchRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AmadeusIntegrationService {

    private static final Logger log = LogManager.getLogger(AmadeusIntegrationService.class);
    private static final String TOKEN_URL  = "https://test.api.amadeus.com/v1/security/oauth2/token";
    private static final String SEARCH_URL = "https://test.api.amadeus.com/v2/shopping/flight-offers";

    private final RestTemplate restTemplate;
    private final SecretManagerService secretManagerService;

    private volatile String cachedToken    = null;
    private volatile long   tokenExpiresAt = 0;

    public AmadeusIntegrationService(RestTemplate restTemplate,
                                     SecretManagerService secretManagerService) {
        this.restTemplate         = restTemplate;
        this.secretManagerService = secretManagerService;
    }

    public List<FlightDto> searchFlights(FlightSearchRequest req) {
        try {
            String token = getAccessToken();

            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                .queryParam("originLocationCode",      req.getOrigin().toUpperCase())
                .queryParam("destinationLocationCode", req.getDestination().toUpperCase())
                .queryParam("departureDate",           req.getDepartureDate().toString())
                .queryParam("adults",                  req.getPassengers())
                .queryParam("travelClass",             toAmadeusCabin(req.getCabinClass()))
                .queryParam("nonStop",                 "false")
                .queryParam("max",                     20)
                .queryParam("currencyCode",            "USD")
                .build()
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<AmadeusOffersResponse> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), AmadeusOffersResponse.class);

            if (resp.getBody() == null || resp.getBody().data == null || resp.getBody().data.isEmpty()) {
                log.info("Amadeus returned 0 offers for {}->{}", req.getOrigin(), req.getDestination());
                return Collections.emptyList();
            }

            List<FlightDto> results = mapOffers(resp.getBody());
            log.info("Amadeus returned {} flights for {}->{}", results.size(), req.getOrigin(), req.getDestination());
            return results;

        } catch (HttpClientErrorException.NotFound e) {
            log.info("Amadeus: no flights found for {}->{} on {}",
                req.getOrigin(), req.getDestination(), req.getDepartureDate());
            return Collections.emptyList();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Amadeus: token rejected — will refresh on next request");
            cachedToken = null;
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Amadeus search failed for {}->{}: {}", req.getOrigin(), req.getDestination(), e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── OAuth2 token (cached until 30s before expiry) ─────────────────────────

    private synchronized String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiresAt - 30_000) {
            return cachedToken;
        }

        String clientId     = secretManagerService.getSecret("AMADEUS_CLIENT_ID");
        String clientSecret = secretManagerService.getSecret("AMADEUS_CLIENT_SECRET");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "client_credentials");
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);

        ResponseEntity<AmadeusTokenResponse> resp = restTemplate.exchange(
            TOKEN_URL, HttpMethod.POST, new HttpEntity<>(body, headers), AmadeusTokenResponse.class);

        AmadeusTokenResponse t = resp.getBody();
        if (t == null || t.accessToken == null) {
            throw new IllegalStateException("Amadeus token response was empty");
        }

        cachedToken    = t.accessToken;
        tokenExpiresAt = now + (t.expiresIn * 1000L);
        log.info("Amadeus token refreshed, valid for {}s", t.expiresIn);
        return cachedToken;
    }

    // ── Response mapping ──────────────────────────────────────────────────────

    private List<FlightDto> mapOffers(AmadeusOffersResponse resp) {
        Map<String, String> carriers = Optional.ofNullable(resp.dictionaries)
            .map(d -> d.carriers).orElse(Map.of());

        List<FlightDto> results = new ArrayList<>();

        for (AmadeusOffer offer : resp.data) {
            try {
                if (offer.itineraries == null || offer.itineraries.isEmpty()) continue;
                AmadeusItinerary itin = offer.itineraries.get(0);
                if (itin.segments == null || itin.segments.isEmpty()) continue;

                AmadeusSegment firstSeg = itin.segments.get(0);
                AmadeusSegment lastSeg  = itin.segments.get(itin.segments.size() - 1);

                Instant dep = parseAt(firstSeg.departure.at);
                Instant arr = parseAt(lastSeg.arrival.at);
                long durationMins = (arr.toEpochMilli() - dep.toEpochMilli()) / 60_000;

                int    stops        = itin.segments.size() - 1;
                String airlineIata  = firstSeg.carrierCode;
                String airlineName  = carriers.getOrDefault(airlineIata, airlineIata);
                String cabin        = extractCabin(offer);
                BigDecimal price    = parsePrice(offer);
                String currency     = offer.price != null ? offer.price.currency : "USD";
                int    seats        = offer.numberOfBookableSeats > 0 ? offer.numberOfBookableSeats : 9;

                FlightDto.FlightDtoBuilder builder = FlightDto.builder()
                    .flightId(UUID.randomUUID())
                    .flightNumber(airlineIata + offer.id)
                    .airlineIata(airlineIata)
                    .airlineName(airlineName)
                    .originIata(firstSeg.departure.iataCode)
                    .destinationIata(lastSeg.arrival.iataCode)
                    .departureTime(dep)
                    .arrivalTime(arr)
                    .durationMinutes((int) durationMins)
                    .availableSeats(seats)
                    .basePrice(price)
                    .currency(currency)
                    .cabinClass(cabin)
                    .source("AMADEUS")
                    .stops(stops);

                if (stops > 0) {
                    String via = itin.segments.get(0).arrival.iataCode;
                    long layover = layoverMins(itin.segments);
                    builder.connectionVia(via).connectionCity(via).layoverMinutes((int) layover);
                }

                results.add(builder.build());
            } catch (Exception e) {
                log.debug("Skipping offer {}: {}", offer.id, e.getMessage());
            }
        }

        return results;
    }

    private Instant parseAt(String at) {
        if (at == null) return Instant.EPOCH;
        try {
            // Amadeus returns "2026-05-16T10:00:00" (local time, no offset)
            return LocalDateTime.parse(at).toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            try { return Instant.parse(at); } catch (Exception ex) { return Instant.EPOCH; }
        }
    }

    private String extractCabin(AmadeusOffer offer) {
        try {
            return offer.travelerPricings.get(0).fareDetailsBySegment.get(0).cabin;
        } catch (Exception e) {
            return "ECONOMY";
        }
    }

    private BigDecimal parsePrice(AmadeusOffer offer) {
        try {
            String raw = offer.price.grandTotal != null ? offer.price.grandTotal : offer.price.total;
            return new BigDecimal(raw);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long layoverMins(List<AmadeusSegment> segs) {
        if (segs.size() < 2) return 0;
        Instant arr = parseAt(segs.get(0).arrival.at);
        Instant dep = parseAt(segs.get(1).departure.at);
        return (dep.toEpochMilli() - arr.toEpochMilli()) / 60_000;
    }

    private String toAmadeusCabin(String cabin) {
        if (cabin == null) return "ECONOMY";
        return switch (cabin.toUpperCase()) {
            case "BUSINESS" -> "BUSINESS";
            case "FIRST"    -> "FIRST";
            case "PREMIUM"  -> "PREMIUM_ECONOMY";
            default         -> "ECONOMY";
        };
    }

    // ── Inner DTO classes (Amadeus response model) ────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusTokenResponse {
        @JsonProperty("access_token") public String accessToken;
        @JsonProperty("expires_in")   public int    expiresIn;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusOffersResponse {
        public List<AmadeusOffer>  data;
        public AmadeusDictionaries dictionaries;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusOffer {
        public String                    id;
        public List<AmadeusItinerary>    itineraries;
        public AmadeusPrice              price;
        public List<AmadeusTravelerPricing> travelerPricings;
        public int                       numberOfBookableSeats;
        public List<String>              validatingAirlineCodes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusItinerary {
        public String               duration;
        public List<AmadeusSegment> segments;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusSegment {
        public AmadeusPoint departure;
        public AmadeusPoint arrival;
        public String       carrierCode;
        public String       number;
        public String       duration;
        public int          numberOfStops;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusPoint {
        public String iataCode;
        public String terminal;
        public String at;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusPrice {
        public String currency;
        public String total;
        public String grandTotal;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusTravelerPricing {
        public List<AmadeusFareDetail> fareDetailsBySegment;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusFareDetail {
        public String cabin;
        public String fareBasis;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmadeusDictionaries {
        public Map<String, String> carriers;
        public Map<String, Object> locations;
        public Map<String, String> aircraft;
        public Map<String, String> currencies;
    }
}