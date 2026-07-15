package com.example.employeemanagement.dto;

import java.time.Instant;

public record ClockOutResponse(
        Long sessionId,
        Long employeeId,
        Instant clockInTime,
        Instant clockOutTime,
        long durationMinutes,
        String message
) {}
