package com.skyways.common.exception.flight;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class FlightNotFoundException extends SkyWaysBaseException {

    public FlightNotFoundException(String flightId) {
        super("FLIGHT_NOT_FOUND", "Flight not found: " + flightId, HttpStatus.NOT_FOUND);
    }

    public FlightNotFoundException(String message, Throwable cause) {
        super("FLIGHT_NOT_FOUND", message, HttpStatus.NOT_FOUND, cause);
    }
}
