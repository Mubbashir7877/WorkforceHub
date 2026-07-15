package com.example.employeemanagement.dto;

import java.time.Instant;

public record TimeClockSessionResponse(
        Long id,
        Long employeeId,
        Long userId,
        Instant clockInTime,
        Instant clockOutTime,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
