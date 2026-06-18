package com.example.employeemanagement.exception;

public class UnknownRoleException extends RuntimeException {
    public UnknownRoleException(String message) {
        super(message);
    }
}
