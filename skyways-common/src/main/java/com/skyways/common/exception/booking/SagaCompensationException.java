package com.skyways.common.exception.booking;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class SagaCompensationException extends SkyWaysBaseException {

    public SagaCompensationException(String sagaId, String step, String reason) {
        super("SAGA_COMPENSATION_FAILED",
              String.format("Saga %s compensation failed at step '%s': %s", sagaId, step, reason),
              HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public SagaCompensationException(String sagaId, String step, Throwable cause) {
        super("SAGA_COMPENSATION_FAILED",
              String.format("Saga %s compensation failed at step '%s'", sagaId, step),
              HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
