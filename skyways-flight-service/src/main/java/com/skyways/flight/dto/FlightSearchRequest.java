package com.skyways.flight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FlightSearchRequest {

    @NotBlank @Size(min = 3, max = 3, message = "IATA code must be exactly 3 characters")
    private String origin;

    @NotBlank @Size(min = 3, max = 3, message = "IATA code must be exactly 3 characters")
    private String destination;

    @NotNull
    private LocalDate departureDate;

    @Min(1)
    private int passengers = 1;

    private String cabinClass = "ECONOMY";

    private int page = 0;
    private int size = 20;
}
