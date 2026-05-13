package com.skyways.common.exception.payment;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class DuplicatePaymentException extends SkyWaysBaseException {

    public DuplicatePaymentException(String idempotencyKey) {
        super("DUPLICATE_PAYMENT",
              "Payment already processed for idempotency key: " + idempotencyKey,
              HttpStatus.CONFLICT);
    }
}
