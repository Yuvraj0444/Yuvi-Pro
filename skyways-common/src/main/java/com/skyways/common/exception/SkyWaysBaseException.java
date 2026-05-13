package com.skyways.common.exception;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;

public abstract class SkyWaysBaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String traceId;

    protected SkyWaysBaseException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.traceId = ThreadContext.get("traceId");
    }

    protected SkyWaysBaseException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.traceId = ThreadContext.get("traceId");
    }

    public String getErrorCode() { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getTraceId() { return traceId; }
}
