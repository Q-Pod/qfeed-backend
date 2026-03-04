package com.ktb.async.core.exception;

public abstract class RetryableException extends RuntimeException {

    protected RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
