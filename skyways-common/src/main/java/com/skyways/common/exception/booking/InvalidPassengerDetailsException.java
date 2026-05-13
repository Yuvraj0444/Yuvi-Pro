package com.skyways.common.exception.booking;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class InvalidPassengerDetailsException extends SkyWaysBaseException {

    private final String field;

    public InvalidPassengerDetailsException(String message, String field) {
        super("INVALID_PASSENGER_DETAILS", message, HttpStatus.BAD_REQUEST);
        this.field = field;
    }

    public InvalidPassengerDetailsException(String message) {
        super("INVALID_PASSENGER_DETAILS", message, HttpStatus.BAD_REQUEST);
        this.field = null;
    }

    public String getField() { return field; }
}
