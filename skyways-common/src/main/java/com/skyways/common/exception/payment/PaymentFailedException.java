package com.skyways.common.exception.payment;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class PaymentFailedException extends SkyWaysBaseException {

    public PaymentFailedException(String message) {
        super("PAYMENT_FAILED", message, HttpStatus.PAYMENT_REQUIRED);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super("PAYMENT_FAILED", message, HttpStatus.PAYMENT_REQUIRED, cause);
    }
}
