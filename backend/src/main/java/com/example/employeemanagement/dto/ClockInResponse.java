package com.example.employeemanagement.dto;

import java.time.Instant;

public record ClockInResponse(
        Long sessionId,
        Long employeeId,
        Instant clockInTime,
        String message
) {}
