package com.ticket.master.common.exception;

public class PostgresErrorCodes {
    private PostgresErrorCodes() {}

    public static final String UNIQUE_VIOLATION = "23505";
    public static final String NOT_NULL_VIOLATION = "23502";
}

