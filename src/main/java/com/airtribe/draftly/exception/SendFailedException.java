package com.airtribe.draftly.exception;

/** Thrown by the Gmail "send" tool when delivery fails (e.g. expired token). */
public class SendFailedException extends RuntimeException {
    public SendFailedException(String message) {
        super(message);
    }
    public SendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
