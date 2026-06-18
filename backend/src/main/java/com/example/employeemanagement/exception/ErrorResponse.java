package com.example.employeemanagement.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        Map<String, String> fieldErrors
) {}
