package com.skyways.booking.controller;

import com.skyways.booking.dto.CreateBookingRequest;
import com.skyways.booking.entity.Booking;
import com.skyways.booking.service.BookingService;
import com.skyways.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Reservation creation with overbooking guard and SAGA orchestration")
@SecurityRequirement(name = "BearerAuth")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Create a booking", description = "Validates passengers, checks seat availability, persists booking in INITIATED state, and triggers the booking SAGA. Requires X-User-Id header (injected by gateway).")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            HttpServletRequest httpRequest) {

        String userIdHeader = httpRequest.getHeader("X-User-Id");
        UUID userId = UUID.fromString(userIdHeader);

        Booking booking = bookingService.createBooking(request, userId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            ApiResponse.ok(Map.of(
                "bookingId",  booking.getBookingId().toString(),
                "bookingRef", booking.getBookingRef(),
                "status",     booking.getStatus().name(),
                "message",    "Booking initiated. Confirmation will be sent to your email."
            ))
        );
    }

    @Operation(summary = "Get booking by reference", description = "Returns current status and amount for a booking reference (e.g. SW-A3K9X)")
    @GetMapping("/{bookingRef}")
    public ResponseEntity<ApiResponse<Map<String, String>>> getBooking(
            @Parameter(description = "Human-readable booking reference, e.g. SW-A3K9X") @PathVariable String bookingRef) {

        Booking booking = bookingService.getByRef(bookingRef);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "bookingId",     booking.getBookingId().toString(),
            "bookingRef",    booking.getBookingRef(),
            "status",        booking.getStatus().name(),
            "totalAmount",   booking.getTotalAmount().toString(),
            "currency",      booking.getCurrency()
        )));
    }
}
