package com.skyways.common.exception.auth;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class TokenExpiredException extends SkyWaysBaseException {

    public TokenExpiredException() {
        super("TOKEN_EXPIRED", "JWT token has expired. Please login again.", HttpStatus.UNAUTHORIZED);
    }

    public TokenExpiredException(String message) {
        super("TOKEN_EXPIRED", message, HttpStatus.UNAUTHORIZED);
    }
}
