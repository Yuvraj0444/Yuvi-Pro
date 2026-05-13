package com.skyways.flight.service;

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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class SkyscannerIntegrationService {

    private static final Logger log = LogManager.getLogger(SkyscannerIntegrationService.class);
    private static final String SKYSCANNER_URL =
        "https://skyscanner-api.p.rapidapi.com/v3/flights/live/search/create";

    private final RestTemplate restTemplate;
    private final SecretManagerService secretManagerService;

    public SkyscannerIntegrationService(RestTemplate restTemplate,
                                         SecretManagerService secretManagerService) {
        this.restTemplate = restTemplate;
        this.secretManagerService = secretManagerService;
    }

    @CircuitBreaker(name = "skyscanner-cb", fallbackMethod = "skyscannerFallback")
    @Retry(name = "skyscanner-retry")
    public CompletableFuture<List<FlightDto>> searchFlightsAsync(FlightSearchRequest req) {
        return CompletableFuture.supplyAsync(() -> searchFlights(req));
    }

    public List<FlightDto> searchFlights(FlightSearchRequest req) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-rapidapi-key", secretManagerService.getSecret("SKYSCANNER_API_KEY"));
            headers.set("x-rapidapi-host", "skyscanner-api.p.rapidapi.com");
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = buildSkyscannerPayload(req);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<SkyscannerSearchResponse> response = restTemplate.exchange(
                SKYSCANNER_URL, HttpMethod.POST, entity, SkyscannerSearchResponse.class);

            if (response.getBody() == null || response.getBody().itineraries == null) {
                return Collections.emptyList();
            }

            return mapSkyscannerResponse(response.getBody());

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new SkyscannerAPIException("Rate limit exceeded on Skyscanner API", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new SkyscannerAPIException("Invalid Skyscanner API key — check Secret Manager", e);
        } catch (HttpClientErrorException e) {
            throw new SkyscannerAPIException("Skyscanner HTTP error: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new SkyscannerAPIException("Skyscanner connection timeout", e);
        } catch (Exception e) {
            throw new SkyscannerAPIException("Unexpected Skyscanner error: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<List<FlightDto>> skyscannerFallback(FlightSearchRequest req, Throwable t) {
        log.warn("Skyscanner circuit breaker fallback for {}->{}: {}",
            req.getOrigin(), req.getDestination(), t.getMessage());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    private String buildSkyscannerPayload(FlightSearchRequest req) {
        return String.format("""
            {
              "query": {
                "market": "US", "locale": "en-US", "currency": "USD",
                "queryLegs": [{
                  "originPlaceId": {"iata": "%s"},
                  "destinationPlaceId": {"iata": "%s"},
                  "date": {"year": %d, "month": %d, "day": %d}
                }],
                "adults": %d,
                "cabinClass": "CABIN_CLASS_%s"
              }
            }""",
            req.getOrigin(), req.getDestination(),
            req.getDepartureDate().getYear(),
            req.getDepartureDate().getMonthValue(),
            req.getDepartureDate().getDayOfMonth(),
            req.getPassengers(),
            req.getCabinClass().toUpperCase()
        );
    }

    private List<FlightDto> mapSkyscannerResponse(SkyscannerSearchResponse resp) {
        if (resp.itineraries == null) return Collections.emptyList();
        return resp.itineraries.stream()
            .map(it -> FlightDto.builder()
                .flightNumber(it.id)
                .basePrice(it.price != null ? new BigDecimal(it.price.amount) : BigDecimal.ZERO)
                .currency("USD")
                .source("SKYSCANNER")
                .build())
            .toList();
    }

    private static class SkyscannerSearchResponse {
        public List<SkyscannerItinerary> itineraries;
    }

    private static class SkyscannerItinerary {
        public String id;
        public SkyscannerPrice price;
    }

    private static class SkyscannerPrice {
        public String amount;
    }
}
