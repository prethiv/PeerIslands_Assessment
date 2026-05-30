package com.peerisland.ecomm.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an order transition is not allowed for its current status.
 * Mapped to HTTP 409 (Conflict).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class IllegalOrderStateException extends RuntimeException {

    public IllegalOrderStateException(String message) {
        super(message);
    }
}
