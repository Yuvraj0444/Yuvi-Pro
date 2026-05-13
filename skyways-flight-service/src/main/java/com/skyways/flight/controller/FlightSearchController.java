package com.skyways.flight.controller;

import com.skyways.common.dto.ApiResponse;
import com.skyways.flight.dto.FlightDto;
import com.skyways.flight.dto.FlightSearchRequest;
import com.skyways.flight.service.FlightAggregatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flights")
@Tag(name = "Flight Search", description = "Aggregated flight search across GDS and Skyscanner with circuit-breaker fallback")
public class FlightSearchController {

    private final FlightAggregatorService aggregatorService;

    public FlightSearchController(FlightAggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }

    @Operation(summary = "Search available flights",
        description = "Searches GDS and Skyscanner, deduplicates results, and ranks by price. No JWT required.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FlightDto>>> searchFlights(
            @Parameter(description = "Search criteria: origin, destination, date, passengers, cabinClass")
            @Valid @ModelAttribute FlightSearchRequest request) {
        List<FlightDto> results = aggregatorService.aggregateSearch(request);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
