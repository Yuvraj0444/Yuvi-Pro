package com.skyways.common.exception.flight;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class SeatAlreadyReservedException extends SkyWaysBaseException {

    public SeatAlreadyReservedException(String seatNumber, String flightId) {
        super("SEAT_ALREADY_RESERVED",
              String.format("Seat %s on flight %s is already reserved", seatNumber, flightId),
              HttpStatus.CONFLICT);
    }
}
