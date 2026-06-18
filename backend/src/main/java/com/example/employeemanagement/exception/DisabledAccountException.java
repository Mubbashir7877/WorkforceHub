package com.example.employeemanagement.exception;

public class DisabledAccountException extends RuntimeException {
    public DisabledAccountException(String message) {
        super(message);
    }
}
