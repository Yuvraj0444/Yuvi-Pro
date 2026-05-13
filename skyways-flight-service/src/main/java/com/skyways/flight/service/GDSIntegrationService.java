package com.skyways.flight.service;

import com.skyways.common.exception.flight.GDSConnectionException;
import com.skyways.common.security.SecretManagerService;
import com.skyways.flight.dto.FlightDto;
import com.skyways.flight.dto.FlightSearchRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
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
public class GDSIntegrationService {

    private static final Logger log = LogManager.getLogger(GDSIntegrationService.class);
    private static final String GDS_BASE_URL = "https://api.gds-provider.com/v2/flights";

    private final RestTemplate restTemplate;
    private final SecretManagerService secretManagerService;

    public GDSIntegrationService(RestTemplate restTemplate,
                                  SecretManagerService secretManagerService) {
        this.restTemplate = restTemplate;
        this.secretManagerService = secretManagerService;
    }

    @CircuitBreaker(name = "gds-cb", fallbackMethod = "gdsFallback")
    @Retry(name = "gds-retry")
    @TimeLimiter(name = "gds-tl")
    public CompletableFuture<List<FlightDto>> searchFlightsAsync(FlightSearchRequest req) {
        return CompletableFuture.supplyAsync(() -> searchFlights(req));
    }

    public List<FlightDto> searchFlights(FlightSearchRequest req) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretManagerService.getSecret("GDS_API_KEY"));
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = String.format("%s?origin=%s&destination=%s&date=%s&passengers=%d",
                GDS_BASE_URL, req.getOrigin(), req.getDestination(),
                req.getDepartureDate(), req.getPassengers());

            ResponseEntity<GDSFlightResponse[]> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), GDSFlightResponse[].class);

            if (response.getBody() == null) {
                return Collections.emptyList();
            }

            return mapGDSResponse(response.getBody());

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new GDSConnectionException("GDS API key rejected — check Secret Manager", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new GDSConnectionException("GDS rate limit exceeded", e);
        } catch (HttpClientErrorException e) {
            throw new GDSConnectionException("GDS HTTP error: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new GDSConnectionException("GDS connection timeout or network error", e);
        } catch (Exception e) {
            throw new GDSConnectionException("Unexpected GDS error: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<List<FlightDto>> gdsFallback(FlightSearchRequest req, Throwable t) {
        log.warn("GDS circuit breaker fallback triggered for {}->{}: {}",
            req.getOrigin(), req.getDestination(), t.getMessage());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    private List<FlightDto> mapGDSResponse(GDSFlightResponse[] responses) {
        return java.util.Arrays.stream(responses)
            .map(r -> FlightDto.builder()
                .flightNumber(r.flightNumber)
                .airlineName(r.airline)
                .originIata(r.origin)
                .destinationIata(r.destination)
                .departureTime(r.departureTime)
                .arrivalTime(r.arrivalTime)
                .availableSeats(r.availableSeats)
                .basePrice(new BigDecimal(r.price))
                .currency(r.currency)
                .source("GDS")
                .build())
            .toList();
    }

    /** Internal DTO for GDS API response deserialization */
    private static class GDSFlightResponse {
        public String flightNumber;
        public String airline;
        public String origin;
        public String destination;
        public java.time.Instant departureTime;
        public java.time.Instant arrivalTime;
        public int availableSeats;
        public String price;
        public String currency;
    }
}
