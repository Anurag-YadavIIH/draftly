package com.airtribe.draftly.exception;

/** Thrown when an operation is not allowed for the resource's current state,
 *  e.g. trying to send a draft that has not been approved. */
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }
}
