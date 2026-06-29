package com.ticket.master.common.exception;

public class UniquenessViolation extends RuntimeException {
    public UniquenessViolation(String message) {
        super(message);
    }
}
