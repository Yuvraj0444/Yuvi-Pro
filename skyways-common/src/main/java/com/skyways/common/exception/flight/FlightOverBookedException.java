package com.skyways.common.exception.flight;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class FlightOverBookedException extends SkyWaysBaseException {

    public FlightOverBookedException(String message) {
        super("FLIGHT_OVERBOOKED", message, HttpStatus.CONFLICT);
    }

    public FlightOverBookedException(String flightId, int requestedSeats, int availableSeats) {
        super("FLIGHT_OVERBOOKED",
              String.format("Flight %s is overbooked: requested %d seats but only %d available",
                            flightId, requestedSeats, availableSeats),
              HttpStatus.CONFLICT);
    }
}
