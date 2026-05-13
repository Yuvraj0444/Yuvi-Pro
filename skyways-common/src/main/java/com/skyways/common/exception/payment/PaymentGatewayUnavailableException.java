package com.skyways.common.exception.payment;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class PaymentGatewayUnavailableException extends SkyWaysBaseException {

    public PaymentGatewayUnavailableException(String message) {
        super("PAYMENT_GATEWAY_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public PaymentGatewayUnavailableException(String message, Throwable cause) {
        super("PAYMENT_GATEWAY_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
