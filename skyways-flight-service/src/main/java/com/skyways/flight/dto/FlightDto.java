package com.skyways.flight.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightDto {
    private UUID flightId;
    private String flightNumber;
    private String airlineName;
    private String airlineIata;
    private String originIata;
    private String originCity;
    private String destinationIata;
    private String destinationCity;
    private Instant departureTime;
    private Instant arrivalTime;
    private int durationMinutes;
    private int availableSeats;
    private BigDecimal basePrice;
    private String currency;
    private String cabinClass;
    private String source;  // GDS | SKYSCANNER | INTERNAL | CONNECTING

    // Populated only for connecting flights (stops > 0)
    private int stops;
    private String connectionVia;       // hub airport IATA e.g. "DEL"
    private String connectionCity;      // hub city name
    private Integer layoverMinutes;
    private List<FlightDto> legs;       // the individual flight legs
}