package com.skyways.common.exception.flight;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class InvalidFlightStateException extends SkyWaysBaseException {

    public InvalidFlightStateException(String flightId, String currentState, String requiredState) {
        super("INVALID_FLIGHT_STATE",
              String.format("Flight %s is in state '%s', required state '%s'",
                            flightId, currentState, requiredState),
              HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
