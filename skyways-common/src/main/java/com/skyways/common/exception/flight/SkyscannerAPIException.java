package com.skyways.common.exception.flight;

public class SkyscannerAPIException extends GDSConnectionException {

    public SkyscannerAPIException(String message) {
        super("Skyscanner API error: " + message);
    }

    public SkyscannerAPIException(String message, Throwable cause) {
        super("Skyscanner API error: " + message, cause);
    }
}
