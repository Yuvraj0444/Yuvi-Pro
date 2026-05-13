package com.skyways.common.exception.auth;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class AuthenticationException extends SkyWaysBaseException {

    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message, HttpStatus.UNAUTHORIZED);
    }
}
