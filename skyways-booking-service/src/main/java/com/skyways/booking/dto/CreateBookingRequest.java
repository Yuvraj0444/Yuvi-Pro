package com.skyways.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateBookingRequest {

    @NotNull
    private UUID flightId;

    @NotNull
    private BigDecimal totalAmount;

    private String currency = "USD";

    private String fareClass = "ECONOMY";

    @NotEmpty
    @Valid
    private List<PassengerDto> passengers;
}
