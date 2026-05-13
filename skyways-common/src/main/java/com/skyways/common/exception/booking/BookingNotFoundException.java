package com.skyways.common.exception.booking;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends SkyWaysBaseException {

    public BookingNotFoundException(String bookingRef) {
        super("BOOKING_NOT_FOUND", "Booking not found: " + bookingRef, HttpStatus.NOT_FOUND);
    }
}
