package com.skyways.flight.service;

import com.skyways.flight.dto.FlightDto;
import com.skyways.flight.dto.FlightSearchRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates flight results from available providers.
 *
 * Current mode: SIMULATED — returns realistic mock data via MockFlightDataService.
 *
 * TODO: When real API credentials are available, replace the body of
 *       fetchFlights() with parallel calls to SkyscannerIntegrationService
 *       and AmadeusIntegrationService (see git history for that wiring).
 */
@Service
public class FlightAggregatorService {

    private static final Logger log = LogManager.getLogger(FlightAggregatorService.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private final MockFlightDataService mockService;

    // TODO: inject SkyscannerIntegrationService + AmadeusIntegrationService here
    //       when switching back to live APIs.

    private final ConcurrentHashMap<String, CachedResult> searchCache = new ConcurrentHashMap<>();

    public FlightAggregatorService(MockFlightDataService mockService) {
        this.mockService = mockService;
    }

    public List<FlightDto> aggregateSearch(FlightSearchRequest req) {
        String cacheKey = buildCacheKey(req);

        CachedResult cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for [{}->{}]", req.getOrigin(), req.getDestination());
            return cached.results();
        }

        List<FlightDto> results = fetchFlights(req);

        if (!results.isEmpty()) {
            searchCache.put(cacheKey, new CachedResult(results));
        }

        return results;
    }

    // ── Provider dispatch ─────────────────────────────────────────────────────

    private List<FlightDto> fetchFlights(FlightSearchRequest req) {
        try {
            // TODO: replace with parallel external API calls when credentials are available:
            //   CompletableFuture<List<FlightDto>> skyF = skyscannerService.searchFlightsAsync(req)...
            //   CompletableFuture<List<FlightDto>> amF  = CompletableFuture.supplyAsync(() -> amadeusService.searchFlights(req))...
            //   CompletableFuture.allOf(skyF, amF).join();
            //   return merge(skyF.join(), amF.join());

            List<FlightDto> results = mockService.searchFlights(req);

            log.info("Flight search {}->{}: {} results",
                req.getOrigin(), req.getDestination(), results.size());

            return results;

        } catch (Exception ex) {
            log.error("Flight search failed for {}->{}: {}",
                req.getOrigin(), req.getDestination(), ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    private String buildCacheKey(FlightSearchRequest req) {
        return req.getOrigin() + ":" + req.getDestination() + ":" +
               req.getDepartureDate() + ":" + req.getPassengers() + ":" + req.getCabinClass();
    }

    private record CachedResult(List<FlightDto> results, long cachedAt) {
        CachedResult(List<FlightDto> results) {
            this(results, System.currentTimeMillis());
        }
        boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > CACHE_TTL_MS;
        }
    }
}