package com.skyways.common.exception.auth;

import com.skyways.common.exception.SkyWaysBaseException;
import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends SkyWaysBaseException {

    public UnauthorizedAccessException(String resource) {
        super("UNAUTHORIZED_ACCESS",
              "You do not have permission to access: " + resource,
              HttpStatus.FORBIDDEN);
    }
}
