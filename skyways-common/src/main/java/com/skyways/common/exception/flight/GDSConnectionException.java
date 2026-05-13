package com.skyways.common.exception.flight;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class GDSConnectionException extends SkyWaysBaseException {

    public GDSConnectionException(String message) {
        super("GDS_CONNECTION_ERROR", message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public GDSConnectionException(String message, Throwable cause) {
        super("GDS_CONNECTION_ERROR", message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
