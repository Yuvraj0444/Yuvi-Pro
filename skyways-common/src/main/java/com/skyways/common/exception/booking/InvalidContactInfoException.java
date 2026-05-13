package com.skyways.common.exception.booking;

public class InvalidContactInfoException extends InvalidPassengerDetailsException {

    public InvalidContactInfoException(String message, String field) {
        super(message, field);
    }
}
