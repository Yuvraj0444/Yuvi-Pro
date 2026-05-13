package com.skyways.common.exception.booking;

public class PassportExpiredException extends InvalidPassengerDetailsException {

    public PassportExpiredException(String expiryDate) {
        super("Passport expired on " + expiryDate + ". A valid passport is required for booking.", "passportExpiry");
    }
}
