package com.example.employeemanagement.exception;

public class TimeClockConflictException extends RuntimeException {

    private final String errorCode;

    public TimeClockConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
