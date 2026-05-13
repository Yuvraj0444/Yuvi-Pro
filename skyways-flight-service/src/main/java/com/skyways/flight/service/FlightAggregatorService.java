package com.skyways.flight.service;

import com.skyways.flight.dto.FlightDto;
import com.skyways.flight.dto.FlightSearchRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Aggregates results from GDS and Skyscanner, deduplicates by flight number,
 * and ranks by base price ascending. Results are cached in-memory for 5 minutes
 * to reduce external API calls on repeated identical searches.
 */
@Service
public class FlightAggregatorService {

    private static final Logger log = LogManager.getLogger(FlightAggregatorService.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final GDSIntegrationService gdsService;
    private final SkyscannerIntegrationService skyscannerService;

    private final ConcurrentHashMap<String, CachedResult> searchCache = new ConcurrentHashMap<>();

    public FlightAggregatorService(GDSIntegrationService gdsService,
                                    SkyscannerIntegrationService skyscannerService) {
        this.gdsService = gdsService;
        this.skyscannerService = skyscannerService;
    }

    public List<FlightDto> aggregateSearch(FlightSearchRequest req) {
        String cacheKey = buildCacheKey(req);

        CachedResult cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for search [{}->{}]", req.getOrigin(), req.getDestination());
            return cached.results();
        }

        try {
            CompletableFuture<List<FlightDto>> gdsFuture =
                gdsService.searchFlightsAsync(req).exceptionally(ex -> {
                    log.warn("GDS search failed: {}", ex.getMessage());
                    return Collections.emptyList();
                });

            CompletableFuture<List<FlightDto>> skyscannerFuture =
                skyscannerService.searchFlightsAsync(req).exceptionally(ex -> {
                    log.warn("Skyscanner search failed: {}", ex.getMessage());
                    return Collections.emptyList();
                });

            CompletableFuture.allOf(gdsFuture, skyscannerFuture).join();

            List<FlightDto> merged = Stream.concat(
                gdsFuture.join().stream(),
                skyscannerFuture.join().stream()
            )
            .collect(Collectors.toMap(
                FlightDto::getFlightNumber,
                f -> f,
                (existing, replacement) ->
                    existing.getBasePrice().compareTo(replacement.getBasePrice()) <= 0
                        ? existing : replacement,
                LinkedHashMap::new
            ))
            .values().stream()
            .sorted(Comparator.comparing(FlightDto::getBasePrice))
            .toList();

            searchCache.put(cacheKey, new CachedResult(merged));

            log.info("Aggregated {} flights for {}->{} (GDS: {}, Skyscanner: {})",
                merged.size(), req.getOrigin(), req.getDestination(),
                gdsFuture.join().size(), skyscannerFuture.join().size());

            return merged;

        } catch (Exception e) {
            log.error("Flight aggregation error for {}->{}", req.getOrigin(), req.getDestination(), e);
            return Collections.emptyList();
        }
    }

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
