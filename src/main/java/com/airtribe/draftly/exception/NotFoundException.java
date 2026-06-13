package com.airtribe.draftly.exception;

/** Thrown when a requested resource (email, draft, etc.) does not exist. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
