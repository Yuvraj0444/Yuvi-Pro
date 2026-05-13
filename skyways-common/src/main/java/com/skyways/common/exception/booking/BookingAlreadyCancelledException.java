package com.skyways.common.exception.booking;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class BookingAlreadyCancelledException extends SkyWaysBaseException {

    public BookingAlreadyCancelledException(String bookingRef) {
        super("BOOKING_ALREADY_CANCELLED",
              "Booking " + bookingRef + " is already cancelled",
              HttpStatus.CONFLICT);
    }
}
