package com.skyways.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BookingConfirmationDto {
    private String passengerEmail;
    private String passengerName;
    private String bookingRef;
    private String flightNumber;
    private String originCity;
    private String destinationCity;
    private String departureTime;
    private String arrivalTime;
    private BigDecimal totalAmount;
    private String currency;
    private String seatNumbers;
    private String eventType;   // CONFIRMED | CANCELLED
}
