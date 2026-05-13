package com.skyways.common.exception.payment;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class RefundFailedException extends SkyWaysBaseException {

    public RefundFailedException(String paymentId, String reason) {
        super("REFUND_FAILED",
              "Refund failed for payment " + paymentId + ": " + reason,
              HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public RefundFailedException(String paymentId, Throwable cause) {
        super("REFUND_FAILED",
              "Refund failed for payment " + paymentId,
              HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }
}
