package com.skyways.common.exception.payment;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class InvalidPaymentAmountException extends SkyWaysBaseException {

    public InvalidPaymentAmountException(String message) {
        super("INVALID_PAYMENT_AMOUNT", message, HttpStatus.BAD_REQUEST);
    }

    public InvalidPaymentAmountException(String message, Throwable cause) {
        super("INVALID_PAYMENT_AMOUNT", message, HttpStatus.BAD_REQUEST, cause);
    }
}
